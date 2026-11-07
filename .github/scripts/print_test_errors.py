import glob
import os
import xml.etree.ElementTree as ET

def main():
    xml_files = glob.glob('app/build/test-results/**/*.xml', recursive=True)
    found_xml_failure = False

    for xml_path in xml_files:
        try:
            tree = ET.parse(xml_path)
            for tc in tree.findall('.//testcase'):
                failure = tc.find('failure')
                if failure is not None:
                    found_xml_failure = True
                    c = tc.attrib.get('classname', 'Unknown')
                    n = tc.attrib.get('name', 'Unknown')
                    msg = failure.attrib.get('message', '')
                    txt = (failure.text or '').strip()
                    first_line = txt.splitlines()[0] if txt else ''
                    print(f"::error title=Test Failed: {c}.{n}::{msg} | {first_line}")
                    print(f"--- FAILURE DETAILS for {c}.{n} ---")
                    print(txt)
                    print("-----------------------------------")
        except Exception as e:
            print(f"Error parsing {xml_path}: {e}")

    # Also parse gradle-test.log if present
    if os.path.exists("gradle-test.log"):
        with open("gradle-test.log", "r", encoding="utf-8", errors="replace") as f:
            lines = f.readlines()
        
        # Look for compilation errors or failure messages
        error_lines = []
        capture = False
        for line in lines:
            if "FAILURE: Build failed with an exception" in line or "What went wrong:" in line or "e: " in line:
                capture = True
            if capture:
                error_lines.append(line)
                if len(error_lines) >= 60:
                    break
        
        if error_lines:
            summary = "".join(error_lines[:15]).strip()
            print(f"::error title=Gradle Build Failure::{summary}")
            print("=== GRADLE FAILURE EXCERPT ===")
            print("".join(error_lines))
            print("==============================")
        else:
            # Print last 50 lines
            last_lines = "".join(lines[-50:]).strip()
            print(f"::error title=Gradle Output Tail::{last_lines[:500]}")
            print("=== GRADLE LAST 50 LINES ===")
            print(last_lines)
            print("============================")

if __name__ == '__main__':
    main()
