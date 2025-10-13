# Snake AI Project Structure

## 📁 Directory Organization

```
Snake-AI/
├── src/                    # Source code
│   ├── MyAgent.java       # Main AI implementation
│   └── manifest.txt       # JAR manifest file
│
├── lib/                    # External libraries
│   └── SnakeRunner.jar    # Game framework
│
├── submissions/            # JAR files for tournament submission
│   ├── MyAgent.jar        # Latest build
│   └── *.jar              # Previous versions with timestamps
│
├── documentation/          # Technical documentation
│   ├── CODE_ANALYSIS.md   # Code analysis and recommendations
│   ├── FIXES_APPLIED.md   # Detailed fix documentation
│   └── PERFORMANCE_ANALYSIS.md  # Performance comparison
│
├── .vscode/               # VS Code configuration
│   ├── launch.json        # Debug configuration
│   └── settings.json      # Project settings
│
├── build.ps1              # Build script (compiles and creates JAR)
├── .gitignore             # Git ignore rules
└── README.md              # This file

## 🚀 Quick Start

### Building the Project
```powershell
# Option 1: Use build script (recommended)
.\build.ps1

# Option 2: Manual build
cd src
javac -cp ".;../lib/*" MyAgent.java
jar cfm MyAgent.jar manifest.txt MyAgent*.class
```

### Running with Debug
- Press `F5` in VS Code
- Or use Run > Start Debugging

### Submitting to Tournament
1. Run `.\build.ps1` to create JAR with timestamp
2. Find your JAR in `submissions/` folder
3. Submit the latest timestamped JAR file

## 🧹 Clean Build

To remove all generated files:
```powershell
cd src
Remove-Item *.class
```

Build script automatically cleans before each build.

## 📊 Current Implementation

### Features
- ✅ BFS pathfinding (prioritized for positive apples)
- ✅ Hamiltonian cycle fallback
- ✅ Opponent profiling system
- ✅ Emergency escape logic
- ✅ Aggressive apple pursuit
- ✅ Defensive combat (avoid equal/larger snakes)

### Performance
- **Target Rank**: 3rd place or better
- **Strengths**: Efficient pathfinding, safe movement patterns
- **Strategy**: Aggressive apple collection, defensive combat

## 🔧 Configuration

### VS Code Tasks
- **Build**: Compiles the project
- **Run**: Executes with DevelopmentAgent for visualization

### Java Settings
- **JDK**: Java 21
- **Classpath**: `lib/SnakeRunner.jar`

## 📝 Notes

- `.class` files are automatically excluded from git
- JAR files in root/src are ignored (only submissions/ tracked)
- Build script creates timestamped backups in submissions/
- Use VS Code debugger for testing with visualization

## 🐛 Troubleshooting

**"The type MyAgent is already defined"**
- Close all backup files in VS Code
- Run `.\build.ps1` for clean build

**"Cannot find SnakeRunner"**
- Verify `lib/SnakeRunner.jar` exists
- Check `.vscode/settings.json` classpath

**Game window doesn't appear**
- Ensure using `extends DevelopmentAgent`
- Run with `-develop` flag (configured in launch.json)
