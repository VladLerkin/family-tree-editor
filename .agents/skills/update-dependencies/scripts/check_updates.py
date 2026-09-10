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

if __name__ == '__main__':
    check_dependencies()
