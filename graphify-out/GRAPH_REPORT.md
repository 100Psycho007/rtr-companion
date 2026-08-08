# Graph Report - C:\Users\psych\Desktop\rtr-companion-main  (2026-08-08)

## Corpus Check
- 26 files · ~9,832 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 101 nodes · 75 edges · 27 communities detected
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 9 edges
2. `RtrGattManager` - 9 edges
3. `MainActivity` - 5 edges
4. `RtrScanner` - 4 edges
5. `PacketLogger` - 4 edges
6. `RawPacket` - 4 edges
7. `PacketAnalyzer` - 3 edges
8. `RtrApplication` - 2 edges
9. `PacketExporter` - 2 edges
10. `ParsedPacket` - 2 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities

### Community 0 - "Community 0"
Cohesion: 0.18
Nodes (2): MainViewModel, PermissionState

### Community 1 - "Community 1"
Cohesion: 0.22
Nodes (1): RtrGattManager

### Community 2 - "Community 2"
Cohesion: 0.25
Nodes (7): Connected, Connecting, ConnectionState, Disconnected, DiscoveringServices, Error, Ready

### Community 3 - "Community 3"
Cohesion: 0.29
Nodes (1): MainActivity

### Community 4 - "Community 4"
Cohesion: 0.29
Nodes (1): RtrScanner

### Community 5 - "Community 5"
Cohesion: 0.33
Nodes (5): Failed, Idle, Scanning, ScanState, Stopped

### Community 6 - "Community 6"
Cohesion: 0.33
Nodes (2): PacketAnalyzer, ParsedPacket

### Community 7 - "Community 7"
Cohesion: 0.4
Nodes (0): 

### Community 8 - "Community 8"
Cohesion: 0.4
Nodes (0): 

### Community 9 - "Community 9"
Cohesion: 0.4
Nodes (1): PacketLogger

### Community 10 - "Community 10"
Cohesion: 0.4
Nodes (1): RawPacket

### Community 11 - "Community 11"
Cohesion: 0.67
Nodes (1): RtrApplication

### Community 12 - "Community 12"
Cohesion: 0.67
Nodes (1): PacketExporter

### Community 13 - "Community 13"
Cohesion: 0.67
Nodes (0): 

### Community 14 - "Community 14"
Cohesion: 1.0
Nodes (0): 

### Community 15 - "Community 15"
Cohesion: 1.0
Nodes (0): 

### Community 16 - "Community 16"
Cohesion: 1.0
Nodes (0): 

### Community 17 - "Community 17"
Cohesion: 1.0
Nodes (1): BleConstants

### Community 18 - "Community 18"
Cohesion: 1.0
Nodes (1): RtrDevice

### Community 19 - "Community 19"
Cohesion: 1.0
Nodes (0): 

### Community 20 - "Community 20"
Cohesion: 1.0
Nodes (0): 

### Community 21 - "Community 21"
Cohesion: 1.0
Nodes (0): 

### Community 22 - "Community 22"
Cohesion: 1.0
Nodes (0): 

### Community 23 - "Community 23"
Cohesion: 1.0
Nodes (0): 

### Community 24 - "Community 24"
Cohesion: 1.0
Nodes (0): 

### Community 25 - "Community 25"
Cohesion: 1.0
Nodes (0): 

### Community 26 - "Community 26"
Cohesion: 1.0
Nodes (0): 

## Knowledge Gaps
- **15 isolated node(s):** `PermissionState`, `BleConstants`, `ConnectionState`, `Disconnected`, `Connecting` (+10 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 14`** (2 nodes): `RtrCompanionApp.kt`, `RtrCompanionApp()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 15`** (2 nodes): `PermissionScreen.kt`, `PermissionScreen()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 16`** (2 nodes): `Theme.kt`, `RtrCompanionTheme()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 17`** (2 nodes): `BleConstants`, `BleConstants.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 18`** (2 nodes): `RtrDevice.kt`, `RtrDevice`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 19`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 20`** (1 nodes): `settings.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 21`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 22`** (1 nodes): `Color.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 23`** (1 nodes): `Type.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 24`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 25`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 26`** (1 nodes): `download-wrapper.ps1`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `RtrGattManager` connect `Community 1` to `Community 8`?**
  _High betweenness centrality (0.014) - this node is a cross-community bridge._
- **What connects `PermissionState`, `BleConstants`, `ConnectionState` to the rest of the system?**
  _15 weakly-connected nodes found - possible documentation gaps or missing edges._