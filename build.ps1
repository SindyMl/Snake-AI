# Snake AI Build Script
# This script compiles MyAgent.java and creates a JAR file for submission

Write-Host "🐍 Building Snake AI..." -ForegroundColor Green

# Navigate to src directory
Set-Location $PSScriptRoot\src

# Clean old class files
Write-Host "Cleaning old files..." -ForegroundColor Yellow
Remove-Item *.class -ErrorAction SilentlyContinue

# Compile Java source
Write-Host "Compiling MyAgent.java..." -ForegroundColor Yellow
javac -cp ".;../lib/*" MyAgent.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Compilation successful!" -ForegroundColor Green
    
    # Create JAR file
    Write-Host "Creating JAR file..." -ForegroundColor Yellow
    jar cfm MyAgent.jar manifest.txt MyAgent.class MyAgent`$Point.class MyAgent`$Snake.class MyAgent`$SnakeProfile.class MyAgent`$PathNode.class
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ JAR created successfully!" -ForegroundColor Green
        
        # Copy to submissions folder with timestamp
        $timestamp = Get-Date -Format "yyyy-MM-dd_HHmm"
        Copy-Item MyAgent.jar "../submissions/MyAgent_$timestamp.jar"
        
        Write-Host "✅ Copied to submissions/MyAgent_$timestamp.jar" -ForegroundColor Green
        Write-Host "🎮 Ready to submit!" -ForegroundColor Cyan
    }
    else {
        Write-Host "❌ JAR creation failed!" -ForegroundColor Red
    }
}
else {
    Write-Host "❌ Compilation failed!" -ForegroundColor Red
}

# Return to root directory
Set-Location $PSScriptRoot
