import urllib.request
import xml.etree.ElementTree as ET
import tomllib
import re
import sys
import ssl
from functools import cmp_to_key

# Disable SSL verification for Python on macOS
ssl._create_default_https_context = ssl._create_unverified_context


# Define standard version parser and comparator
def parse_version(v):
    parts = re.split(r'[-.]', v)
    parsed = []
    for p in parts:
        if p.isdigit():
            parsed.append((0, int(p)))
        else:
            subparts = re.findall(r'([a-zA-Z]+)|(\d+)', p)
            for s in subparts:
                if s[0]:
                    parsed.append((1, s[0].lower()))
                elif s[1]:
                    parsed.append((0, int(s[1])))
    return parsed

def compare_versions(v1, v2):
    p1 = parse_version(v1)
    p2 = parse_version(v2)
    
    for item1, item2 in zip(p1, p2):
        if item1 != item2:
            if item1[0] == 0 and item2[0] == 1:
                return 1
            elif item1[0] == 1 and item2[0] == 0:
                return -1
            else:
                if item1[1] < item2[1]:
                    return -1
                else:
                    return 1
                    
    if len(p1) < len(p2):
        next_part = p2[len(p1)]
        if next_part[0] == 1:
            return 1
        else:
            return -1
    elif len(p1) > len(p2):
        next_part = p1[len(p2)]
        if next_part[0] == 1:
            return -1
        else:
            return 1
    return 0

def is_stable(v):
    v_lower = v.lower()
    for word in ['alpha', 'beta', 'rc', 'dev', 'm', 'preview', 'snapshot', 'b', 'eap', 'milestone']:
        if word in v_lower:
            if word == 'm':
                if re.search(r'\b[m]\d+|\-[m]\b', v_lower):
                    return False
            else:
                return False
    return True

# Helper to fetch metadata from a URL
def fetch_metadata(url):
    req = urllib.request.Request(
        url, 
        headers={'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36'}
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            return response.read()
    except Exception as e:
        return None

# Get all versions from Maven repositories
def get_versions(group, artifact):
    group_path = group.replace('.', '/')
    repos = [
        ("Google", f"https://dl.google.com/dl/android/maven2/{group_path}/{artifact}/maven-metadata.xml"),
        ("Maven Central", f"https://repo1.maven.org/maven2/{group_path}/{artifact}/maven-metadata.xml"),
        ("Gradle Plugins", f"https://plugins.gradle.org/m2/{group_path}/{artifact}/maven-metadata.xml")
    ]
    
    for name, url in repos:
        xml_data = fetch_metadata(url)
        if xml_data:
            try:
                root = ET.fromstring(xml_data)
                version_elements = root.findall('.//versioning/versions/version')
                versions = [elem.text for elem in version_elements if elem.text]
                if versions:
                    return versions, name
            except Exception as e:
                pass
    return [], None

def check_dependencies():
    with open('gradle/libs.versions.toml', 'rb') as f:
        toml_data = tomllib.load(f)
        
    versions_map = toml_data.get('versions', {})
    libraries = toml_data.get('libraries', {})
    plugins = toml_data.get('plugins', {})
    
    # Map from version name to coordinate list
    version_to_coords = {}
    
    # Add libraries to map
    for lib_key, lib_val in libraries.items():
        if isinstance(lib_val, dict):
            group = lib_val.get('group')
            name = lib_val.get('name')
            module = lib_val.get('module')
            if module and not group:
                parts = module.split(':')
                if len(parts) == 2:
                    group, name = parts[0], parts[1]
            
            version_info = lib_val.get('version')
            version_ref = None
            if isinstance(version_info, dict):
                version_ref = version_info.get('ref')
            elif 'version.ref' in lib_val:
                version_ref = lib_val['version.ref']
                
            if group and name and version_ref:
                if version_ref not in version_to_coords:
                    version_to_coords[version_ref] = []
                version_to_coords[version_ref].append(('library', group, name))
                
    # Add plugins to map
    for plugin_key, plugin_val in plugins.items():
        if isinstance(plugin_val, dict):
            plugin_id = plugin_val.get('id')
            version_info = plugin_val.get('version')
            version_ref = None
            if isinstance(version_info, dict):
                version_ref = version_info.get('ref')
            elif 'version.ref' in plugin_val:
                version_ref = plugin_val['version.ref']
                
            if plugin_id and version_ref:
                if version_ref not in version_to_coords:
                    version_to_coords[version_ref] = []
                # Map plugin to its marker artifact
                group = plugin_id
                name = f"{plugin_id}.gradle.plugin"
                version_to_coords[version_ref].append(('plugin', group, name))

    print(f"{'Version Ref':<25} | {'Current':<15} | {'Latest Stable':<15} | {'Latest Overall':<15} | {'Status':<10} | {'Sources / Coords'}")
    print("-" * 120)
    
    for v_ref, current_ver in versions_map.items():
        # Skip SDK / target / compile versions and local versions
        if v_ref in ['android-compileSdk', 'android-minSdk', 'android-targetSdk', 'java', 'app-version', 'app-versionCode']:
            continue
            
        coords = version_to_coords.get(v_ref, [])
        if not coords:
            print(f"{v_ref:<25} | {current_ver:<15} | {'-':<15} | {'-':<15} | {'Unused':<10} | No associated libraries/plugins in catalog")
            continue
            
        # Check the first coordinate associated with this version reference (they are usually identical in versioning)
        # We can scan all of them if we want to be safe, but they reference the same version ref, so they should be checked together.
        coord_type, group, name = coords[0]
        
        # Special case: KMP is a complex ecosystem. Let's see what versions we find
        all_versions, repo_name = get_versions(group, name)
        
        if not all_versions:
            # Try other coordinates if first one fails
            for alt_type, alt_group, alt_name in coords[1:]:
                all_versions, repo_name = get_versions(alt_group, alt_name)
                if all_versions:
                    group, name = alt_group, alt_name
                    break
                    
        if not all_versions:
            print(f"{v_ref:<25} | {current_ver:<15} | {'Not Found':<15} | {'Not Found':<15} | {'Error':<10} | Could not fetch maven metadata for {group}:{name}")
            continue
            
        # Sort versions using our comparator
        sorted_versions = sorted(all_versions, key=cmp_to_key(compare_versions))
        
        stable_versions = [v for v in sorted_versions if is_stable(v)]
        
        latest_stable = stable_versions[-1] if stable_versions else None
        latest_overall = sorted_versions[-1]
        
        # Compare current version
        status = "Up-to-date"
        if latest_stable and compare_versions(current_ver, latest_stable) < 0:
            status = "Update Avail"
        elif compare_versions(current_ver, latest_overall) < 0:
            status = "Pre-rel Avail"
            
        # Display results
        coord_str = f"{group}:{name} ({repo_name})"
        latest_stable_str = latest_stable if latest_stable else "None"
        print(f"{v_ref:<25} | {current_ver:<15} | {latest_stable_str:<15} | {latest_overall:<15} | {status:<12} | {coord_str}")

def check_github_actions():
    import glob
    import json

    workflow_files = glob.glob('.github/workflows/*.yml') + glob.glob('.github/workflows/*.yaml')
    if not workflow_files:
        return

    actions_found = {}
    for path in workflow_files:
        with open(path, 'r', encoding='utf-8') as f:
            for line in f:
                m = re.search(r'uses:\s*([a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+(?:/[a-zA-Z0-9_.-]+)?)\s*@\s*([a-zA-Z0-9_.-]+)', line)
                if m:
                    full_action, current_ver = m.group(1), m.group(2)
                    parts = full_action.split('/')
                    gh_repo = f"{parts[0]}/{parts[1]}"
                    if full_action not in actions_found:
                        actions_found[full_action] = {
                            'current_ver': current_ver,
                            'gh_repo': gh_repo,
                            'files': set()
                        }
                    actions_found[full_action]['files'].add(path)

    if not actions_found:
        return

    print("\n" + "=" * 120)
    print("GitHub Actions Workflows Dependencies")
    print("=" * 120)
    print(f"{'Action':<35} | {'Current':<15} | {'Latest Release':<15} | {'Latest Major':<15} | {'Status':<12} | {'Files'}")
    print("-" * 120)

    for full_action, info in sorted(actions_found.items()):
        gh_repo = info['gh_repo']
        current_ver = info['current_ver']
        files_str = ", ".join(sorted(info['files']))

        url = f"https://api.github.com/repos/{gh_repo}/releases/latest"
        req = urllib.request.Request(
            url,
            headers={'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)'}
        )
        latest_tag = None
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                latest_tag = data.get('tag_name')
        except Exception:
            pass

        if not latest_tag:
            # Fallback to tags API if no releases
            tags_url = f"https://api.github.com/repos/{gh_repo}/tags?per_page=1"
            req_tags = urllib.request.Request(
                tags_url,
                headers={'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)'}
            )
            try:
                with urllib.request.urlopen(req_tags, timeout=10) as resp:
                    data = json.loads(resp.read().decode('utf-8'))
                    if data and isinstance(data, list) and len(data) > 0:
                        latest_tag = data[0].get('name')
            except Exception:
                pass

        if not latest_tag:
            print(f"{full_action:<35} | {current_ver:<15} | {'Error':<15} | {'Error':<15} | {'Error':<12} | {files_str}")
            continue

        # Extract major version (e.g. "v7" from "v7.0.1")
        major_match = re.search(r'(v\d+)', latest_tag)
        latest_major = major_match.group(1) if major_match else latest_tag

        status = "Up-to-date"
        curr_clean = current_ver.lstrip('v')
        latest_clean = latest_major.lstrip('v')
        if curr_clean.isdigit() and latest_clean.isdigit():
            if int(curr_clean) < int(latest_clean):
                status = "Update Avail"
        elif current_ver != latest_major and current_ver != latest_tag:
            status = "Update Avail"

        print(f"{full_action:<35} | {current_ver:<15} | {latest_tag:<15} | {latest_major:<15} | {status:<12} | {files_str}")

if __name__ == '__main__':
    check_dependencies()
    check_github_actions()
