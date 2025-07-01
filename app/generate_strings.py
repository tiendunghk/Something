import csv
import requests
import json
import re
import os
import sys
import argparse
from typing import List

# Set up console encoding for Windows
if sys.platform.startswith('win'):
    sys.stdout.reconfigure(encoding='utf-8')

# ==== DEFAULT CONFIG ====
DEFAULT_SHEET_ID = "1x7XMEJYv6jVppuA3fV-Srp5OmyrCgdl8w1m0ETsxzxw"
DEFAULT_GID = "0"
DEFAULT_OUTPUT_FILE = "StringApp.kt"
DEFAULT_PACKAGE_NAME = "com.example.myapplication"
DEFAULT_OUTPUT_DIR = "build/generated/source/buildConfig/debug"

def parse_arguments():
    """Parse command line arguments"""
    parser = argparse.ArgumentParser(
        description="Generate Android Kotlin data class from Google Sheets",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python generate_strings.py -p com.myapp.package -o src/main/kotlin
  python generate_strings.py --package com.example.app --output-dir app/src/main/java
  python generate_strings.py -s 1ABC123xyz -p com.myapp -o generated/kotlin
        """
    )

    parser.add_argument(
        '-p', '--package',
        dest='package_name',
        default=DEFAULT_PACKAGE_NAME,
        help=f'Package name for the generated Kotlin class (default: {DEFAULT_PACKAGE_NAME})'
    )

    parser.add_argument(
        '-o', '--output-dir',
        dest='output_dir',
        default=DEFAULT_OUTPUT_DIR,
        help=f'Output directory for the generated file (default: {DEFAULT_OUTPUT_DIR})'
    )

    parser.add_argument(
        '-s', '--sheet-id',
        dest='sheet_id',
        default=DEFAULT_SHEET_ID,
        help=f'Google Sheets ID (default: {DEFAULT_SHEET_ID})'
    )

    parser.add_argument(
        '-g', '--gid',
        dest='gid',
        default=DEFAULT_GID,
        help=f'Sheet GID/tab ID (default: {DEFAULT_GID})'
    )

    parser.add_argument(
        '-f', '--filename',
        dest='filename',
        default=DEFAULT_OUTPUT_FILE,
        help=f'Output filename (default: {DEFAULT_OUTPUT_FILE})'
    )

    return parser.parse_args()

def to_camel_case(name: str) -> str:
    """Convert string to camelCase for Kotlin property names"""
    if not name:
        return "defaultKey"

    # Split by underscores, spaces, and other delimiters
    words = re.split(r'[_\s\-\.]+', name.strip())

    # Filter out empty strings
    words = [word for word in words if word]

    if not words:
        return "defaultKey"

    # First word lowercase, rest title case
    camel_case = words[0].lower()
    for word in words[1:]:
        # Capitalize first letter, keep rest as is
        camel_case += word.capitalize()

    # Ensure it doesn't start with a number
    if camel_case and camel_case[0].isdigit():
        camel_case = f"key{camel_case.capitalize()}"

    # Ensure it's not a Kotlin keyword
    kotlin_keywords = {
        'class', 'fun', 'val', 'var', 'if', 'else', 'when', 'for', 'while',
        'do', 'try', 'catch', 'finally', 'throw', 'return', 'break', 'continue',
        'object', 'interface', 'package', 'import', 'as', 'is', 'in', 'out',
        'by', 'where', 'init', 'constructor', 'this', 'super', 'null', 'true', 'false'
    }

    if camel_case.lower() in kotlin_keywords:
        camel_case = f"{camel_case}Value"

    return camel_case if camel_case else "defaultKey"

def download_sheet_data(sheet_id: str, gid: str) -> List[str]:
    """Download and parse CSV data from Google Sheets"""
    try:
        print("📡 Attempting to download from Google Sheets...")

        # Try CSV export first (more reliable than JSON)
        csv_url = f"https://docs.google.com/spreadsheets/d/{sheet_id}/export?format=csv&gid={gid}"

        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }

        response = requests.get(csv_url, headers=headers, timeout=30)
        response.raise_for_status()
        response.encoding = "utf-8"

        if not response.text.strip():
            raise Exception("Empty response from Google Sheets")

        print("✅ Successfully downloaded sheet data")

        # Parse CSV data
        lines = response.text.splitlines()
        if not lines:
            raise Exception("No data in CSV response")

        reader = csv.DictReader(lines)

        # Get the first column name (assumed to contain keys)
        first_column = list(reader.fieldnames)[0] if reader.fieldnames else None
        if not first_column:
            raise Exception("No columns found in CSV")

        print(f"📋 Reading keys from column: '{first_column}'")

        keys = []
        for row_num, row in enumerate(reader, start=2):  # Start from 2 since header is row 1
            key_value = row.get(first_column, '').strip()
            if key_value:
                keys.append(key_value)
                print(f"   Row {row_num}: {key_value}")

        if not keys:
            raise Exception("No valid keys found in the sheet")

        return keys

    except requests.exceptions.RequestException as e:
        print(f"❌ Network error: {e}")
        return []
    except Exception as e:
        print(f"❌ Error downloading sheet data: {e}")
        return []

def generate_kotlin_data_class(keys: List[str], package_name: str, sheet_id: str) -> str:
    """Generate Kotlin data class with @Json annotations for Moshi and camelCase properties"""

    # Remove duplicates while preserving order
    unique_keys = []
    seen = set()
    for key in keys:
        if key not in seen:
            unique_keys.append(key)
            seen.add(key)

    kotlin_lines = [
        f"package {package_name}",
        "",
        "import com.squareup.moshi.Json",
        "import com.squareup.moshi.JsonClass",
        "",
        "/**",
        " * Auto-generated string constants data class",
        " * Generated from Google Sheets",
        f" * Sheet ID: {sheet_id}",
        f" * Total unique keys: {len(unique_keys)}",
        " * ",
        " * DO NOT EDIT THIS FILE MANUALLY",
        " * Run the Python generator script to update",
        " */",
        "@JsonClass(generateAdapter = true)",
        "data class StringApp("
    ]

    # Track property names to avoid duplicates
    property_names = {}
    properties = []

    for i, key in enumerate(unique_keys):
        camel_case_name = to_camel_case(key)

        # Handle duplicate camelCase property names
        original_name = camel_case_name
        counter = 1
        while camel_case_name in property_names:
            camel_case_name = f"{original_name}{counter}"
            counter += 1

        property_names[camel_case_name] = key

        # Generate property with Moshi @Json annotation and default value
        property_line = f'    @Json(name = "{key}") val {camel_case_name}: String = "{key}"'
        properties.append(property_line)

    # Join properties with commas
    kotlin_lines.extend([prop + ("," if i < len(properties) - 1 else "") for i, prop in enumerate(properties)])

    kotlin_lines.extend([
        ")",
        "",
        "// Property mapping for reference:",
        "// Original Key -> Kotlin Property"
    ])

    # Add comments showing the mapping
    for prop_name, original_key in property_names.items():
        kotlin_lines.append(f"// \"{original_key}\" -> {prop_name}")

    return "\n".join(kotlin_lines)

def create_output_directory(output_dir: str):
    """Create the output directory structure if it doesn't exist"""
    try:
        os.makedirs(output_dir, exist_ok=True)
        print(f"📁 Created/verified directory: {output_dir}")
        return True
    except Exception as e:
        print(f"❌ Error creating directory {output_dir}: {e}")
        return False

def write_kotlin_file(kotlin_code: str, output_dir: str, filename: str) -> bool:
    """Write the generated Kotlin code to file"""
    try:
        # Write to the specified output directory
        full_path = os.path.join(output_dir, filename)

        with open(full_path, "w", encoding="utf-8") as f:
            f.write(kotlin_code)

        print(f"✅ Generated {full_path}")

        # Also write to current directory for convenience
        with open(filename, "w", encoding="utf-8") as f:
            f.write(kotlin_code)

        print(f"✅ Also saved to current directory: {filename}")
        return True

    except Exception as e:
        print(f"❌ Error writing Kotlin file: {e}")
        return False

def generate_usage_example(keys: List[str], package_name: str):
    """Generate and display usage examples"""
    print("\n" + "="*60)
    print("🔧 USAGE EXAMPLES")
    print("="*60)

    print("\n1. Add to your build.gradle (app level):")
    print("   dependencies {")
    print("       implementation 'com.squareup.moshi:moshi:1.15.0'")
    print("       implementation 'com.squareup.moshi:moshi-kotlin:1.15.0'")
    print("       kapt 'com.squareup.moshi:moshi-kotlin-codegen:1.15.0'")
    print("       // OR if using KSP instead of kapt:")
    print("       // ksp 'com.squareup.moshi:moshi-kotlin-codegen:1.15.0'")
    print("   }")
    print("\n   // Also add to the top of your build.gradle:")
    print("   apply plugin: 'kotlin-kapt'")
    print("   // OR if using KSP:")
    print("   // id 'com.google.devtools.ksp' version '1.9.0-1.0.13'")

    print("\n2. Import and use the generated data class:")
    print(f"   import {package_name}.StringApp")
    print("   val moshi = Moshi.Builder().build()")
    print("   val adapter = moshi.adapter(StringApp::class.java)")
    print("   val stringApp = adapter.fromJson(jsonString)")

    print("\n3. Access properties:")
    if keys:
        for i, key in enumerate(keys[:3]):  # Show first 3 examples
            camel_case = to_camel_case(key)
            print(f"   // '{key}' -> stringApp?.{camel_case}")
        if len(keys) > 3:
            print(f"   // ... and {len(keys) - 3} more properties")

    print("\n4. Create JSON for testing:")
    if keys:
        sample_json = {key: f"Value for {key}" for key in keys[:3]}
        print("   val jsonString = \"\"\"")
        print(f"   {json.dumps(sample_json, indent=4)}")
        print("   \"\"\".trimIndent()")

    print("\n5. Convert to JSON:")
    print("   val jsonString = adapter.toJson(stringApp)")

    print("\n6. Run this script again to regenerate:")
    print("   python3 generate_strings.py -p your.package.name -o your/output/dir")

    print("\n💡 Note: The @JsonClass(generateAdapter = true) annotation")
    print("   automatically generates the Moshi adapter at compile time!")

def main():
    """Main execution function"""
    # Parse command line arguments
    args = parse_arguments()

    try:
        print("Starting Android String Constants Generator")
        print(f"Package: {args.package_name}")
        print(f"Sheet ID: {args.sheet_id}")
        print(f"Output: {args.output_dir}/{args.filename}")
        print("-" * 50)
    except UnicodeEncodeError:
        # Fallback to ASCII if Unicode fails
        print("Starting Android String Constants Generator")
        print(f"Package: {args.package_name}")
        print(f"Sheet ID: {args.sheet_id}")
        print(f"Output: {args.output_dir}/{args.filename}")
        print("-" * 50)

    # Download data from Google Sheets
    keys = download_sheet_data(args.sheet_id, args.gid)

    if not keys:
        print("❌ No keys found or failed to download data")
        print("\n💡 Troubleshooting:")
        print("   1. Check if the Google Sheet is publicly accessible")
        print("   2. Verify the SHEET_ID is correct")
        print("   3. Ensure the first column contains your string keys")
        print("   4. Check your internet connection")
        print(f"\n   Current Sheet URL: https://docs.google.com/spreadsheets/d/{args.sheet_id}")
        return False

    print(f"📝 Found {len(keys)} total keys")

    # Create output directory
    if not create_output_directory(args.output_dir):
        return False

    # Generate Kotlin code
    print("⚙️  Generating Kotlin data class...")
    kotlin_code = generate_kotlin_data_class(keys, args.package_name, args.sheet_id)

    # Write to files
    if not write_kotlin_file(kotlin_code, args.output_dir, args.filename):
        return False

    # Show usage examples
    generate_usage_example(keys, args.package_name)

    print(f"\n🎉 Successfully generated {args.filename} with {len(set(keys))} unique properties!")
    print(f"📦 Package: {args.package_name}")
    print(f"📁 Location: {args.output_dir}/{args.filename}")
    return True

if __name__ == "__main__":
    success = main()
    if not success:
        exit(1)