#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const [root261, root262] = process.argv.slice(2);
if (!root261 || !root262) {
    console.error(
        "Usage: node scripts/audit-structure-goals.mjs <26.1.2 loot-table directory> <26.2 loot-table directory>"
    );
    process.exit(2);
}

const projectRoot = path.resolve(import.meta.dirname, "..");
const categorySource = fs.readFileSync(
    path.join(
        projectRoot,
        "src/main/java/dev/playmonkeei/allandonlychests/challenge/StructureCategory.java"
    ),
    "utf8"
);
const configuredGoals = readSimpleGoalYaml(
    path.join(projectRoot, "src/main/resources/structure-goals.yml")
);
const categories = readCategories(categorySource);

const audited261 = auditVersion(root261, categories);
const audited262 = auditVersion(root262, categories);
let failureCount = 0;

console.log("Kategorie                 26.1.2  26.2  YAML  Ergebnis");
console.log("-------------------------------------------------------");
for (const category of categories) {
    const items261 = audited261.get(category.id);
    const items262 = audited262.get(category.id);
    const configured = configuredGoals.get(category.id) ?? new Set();
    const versionDifference = symmetricDifference(items261, items262);
    const expectedConfig = expectedConfiguredMaterials(category.id, items262);
    const missing = difference(expectedConfig, configured);
    const extra = difference(configured, expectedConfig);
    const okay = missing.size === 0 && extra.size === 0;

    if (!okay) {
        failureCount++;
    }
    console.log(
        `${category.id.padEnd(25)} ${String(items261.size).padStart(6)}`
            + ` ${String(items262.size).padStart(5)}`
            + ` ${String(configured.size).padStart(5)}  ${okay ? "OK" : "ABWEICHUNG"}`
    );
    printDifference("Versionsunterschied (wird zur Laufzeit gefiltert)", versionDifference);
    printDifference("fehlt in YAML", missing);
    printDifference("zusätzlich in YAML", extra);
}

if (failureCount > 0) {
    console.error(`\nAudit fehlgeschlagen: ${failureCount} Kategorie(n) mit Abweichungen.`);
    process.exit(1);
}
console.log("\nAudit erfolgreich: alle Kategorien stimmen semantisch überein.");

function readCategories(source) {
    const enumBody = source.slice(
        source.indexOf("public enum StructureCategory"),
        source.indexOf("private static final int EXPECTED_VANILLA_LOOT_TABLES")
    );
    const blockPattern = /^\s{4}([A-Z_]+)\(\n([\s\S]*?)\n\s{4}\)(?:,|;)/gm;
    const result = [];
    for (const match of enumBody.matchAll(blockPattern)) {
        const strings = [...match[2].matchAll(/"([^"]+)"/g)].map(value => value[1]);
        result.push({
            enumName: match[1],
            id: strings[0],
            lootTables: strings.slice(2)
        });
    }
    if (result.length !== 18) {
        throw new Error(`Expected 18 categories, parsed ${result.length}`);
    }
    return result;
}

function readSimpleGoalYaml(file) {
    const result = new Map();
    let current = null;
    for (const rawLine of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
        const line = rawLine.replace(/\s+#.*$/, "");
        const category = /^([a-z0-9_]+):\s*$/.exec(line);
        if (category) {
            current = new Set();
            result.set(category[1], current);
            continue;
        }
        const item = /^\s+-\s+([a-z0-9_]+)\s*$/.exec(line);
        if (item && current) {
            current.add(item[1]);
        }
    }
    return result;
}

function auditVersion(root, categoryDefinitions) {
    const result = new Map();
    const cache = new Map();
    for (const category of categoryDefinitions) {
        const items = new Set();
        for (const table of category.lootTables) {
            collectTableItems(root, `chests/${table}`, items, cache, new Set());
        }
        if (category.id === "trial_chambers") {
            for (const table of [
                "spawners/trial_chamber/consumables",
                "spawners/trial_chamber/key",
                "spawners/ominous/trial_chamber/consumables",
                "spawners/ominous/trial_chamber/key"
            ]) {
                collectTableItems(root, table, items, cache, new Set());
            }
        }
        result.set(category.id, items);
    }
    return result;
}

function collectTableItems(root, tableName, destination, cache, resolving) {
    if (resolving.has(tableName)) {
        throw new Error(`Recursive loot-table reference: ${tableName}`);
    }
    resolving.add(tableName);
    const document = readLootTable(root, tableName, cache);
    walkLootNode(document, [], (entry, inheritedFunctions) => {
        if (entry.type === "minecraft:item") {
            const rawMaterial = stripMinecraftNamespace(entry.name);
            destination.add(
                transformedMaterial(rawMaterial, [...inheritedFunctions, ...(entry.functions ?? [])])
            );
        } else if (entry.type === "minecraft:loot_table") {
            collectTableItems(
                root,
                stripLootTablePrefix(entry.value ?? entry.name),
                destination,
                cache,
                resolving
            );
        }
    });
    resolving.delete(tableName);
}

function readLootTable(root, tableName, cache) {
    if (!cache.has(tableName)) {
        const file = path.join(root, `${tableName}.json`);
        cache.set(tableName, JSON.parse(fs.readFileSync(file, "utf8")));
    }
    return cache.get(tableName);
}

function walkLootNode(node, inheritedFunctions, visit) {
    if (!node || typeof node !== "object") {
        return;
    }
    const functions = [...inheritedFunctions, ...(node.functions ?? [])];
    if (typeof node.type === "string") {
        visit(node, inheritedFunctions);
    }
    for (const key of ["pools", "entries", "children"]) {
        for (const child of node[key] ?? []) {
            walkLootNode(child, functions, visit);
        }
    }
}

function transformedMaterial(material, functions) {
    const functionNames = new Set(
        functions.map(value => typeof value === "string" ? value : value.function)
    );
    if (
        material === "book"
        && (
            functionNames.has("minecraft:enchant_randomly")
            || functionNames.has("minecraft:enchant_with_levels")
            || functionNames.has("minecraft:set_enchantments")
        )
    ) {
        return "enchanted_book";
    }
    if (material === "map" && functionNames.has("minecraft:exploration_map")) {
        return "filled_map";
    }
    return material;
}

function expectedConfiguredMaterials(categoryId, officialItems) {
    const result = new Set(officialItems);
    if (categoryId === "trial_chambers") {
        result.delete("potion");
        result.delete("tipped_arrow");
    }
    if (categoryId === "jungle_temple") {
        // The arrow table belongs to a trap dispenser. Like the original
        // challenge, natural dispensers are not lootable containers.
        result.delete("arrow");
    }
    if (categoryId === "bastion") {
        result.delete("diamond_pickaxe");
    }
    return result;
}

function stripMinecraftNamespace(value) {
    return value.replace(/^minecraft:/, "");
}

function stripLootTablePrefix(value) {
    return stripMinecraftNamespace(value);
}

function difference(left, right) {
    return new Set([...left].filter(value => !right.has(value)));
}

function symmetricDifference(left, right) {
    return new Set([...difference(left, right), ...difference(right, left)]);
}

function printDifference(label, values) {
    if (values.size > 0) {
        console.log(`  - ${label}: ${[...values].sort().join(", ")}`);
    }
}
