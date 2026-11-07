import glob
import os
import xml.etree.ElementTree as ET

def main():
    xml_files = glob.glob('app/build/test-results/**/*.xml', recursive=True)
    if not xml_files:
        print("No test result XML files found.")
        return

    failed = False
    for xml_path in xml_files:
        try:
            tree = ET.parse(xml_path)
            for tc in tree.findall('.//testcase'):
                failure = tc.find('failure')
                if failure is not None:
                    failed = True
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

    if not failed:
        print("All test XMLs indicate success or no failures found.")

if __name__ == '__main__':
    main()
