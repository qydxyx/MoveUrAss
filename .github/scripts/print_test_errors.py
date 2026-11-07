import glob
import os
import xml.etree.ElementTree as ET

def escape_gh(s: str) -> str:
    return s.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')

def main():
    xml_files = glob.glob('app/build/test-results/**/*.xml', recursive=True)
    for xml_path in xml_files:
        try:
            tree = ET.parse(xml_path)
            for tc in tree.findall('.//testcase'):
                failure = tc.find('failure')
                if failure is not None:
                    c = tc.attrib.get('classname', 'Unknown')
                    n = tc.attrib.get('name', 'Unknown')
                    msg = failure.attrib.get('message', '')
                    txt = (failure.text or '').strip()
                    payload = f"{msg}\n{txt}"
                    print(f"::error title=Test Failed: {c}.{n}::{escape_gh(payload[:2000])}")
        except Exception as e:
            print(f"Error parsing {xml_path}: {e}")

    if os.path.exists("gradle-test.log"):
        with open("gradle-test.log", "r", encoding="utf-8", errors="replace") as f:
            lines = f.readlines()

        # Find where failure begins
        fail_idx = -1
        for i, line in enumerate(lines):
            if "FAILURE: Build failed with an exception" in line or "e: " in line:
                fail_idx = i
                break

        if fail_idx != -1:
            excerpt = "".join(lines[fail_idx : fail_idx + 60])
        else:
            excerpt = "".join(lines[-60:])

        print(f"::error title=Gradle Failure Details::{escape_gh(excerpt[:3000])}")

if __name__ == '__main__':
    main()
