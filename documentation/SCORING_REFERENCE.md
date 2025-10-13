# Advanced Strategy Scoring Reference

## 📊 Quick Scoring Breakdown

### **Move Evaluation Scoring System**

Each potential move is scored by combining multiple strategy layers:

---

## 🎯 Strategy Scores (Additive)

### **1. Wall Hugging Defense**
```
Distance 1-3 from wall:  +25
Distance 0 (touching):   -10
Distance 4+:              -5
```

### **2. Spiral/Corkscrew Pattern** (Length ≥ 10)
```
Moving toward center:    +15
Moving outward:          +10
Maintaining distance:     +5
```

### **3. Body Guard Formation** (Length ≥ 15)
```
Per protected space:      +8
(Max 4 directions × 8 = +32 possible)
```

### **4. Apple Risk/Reward**

#### Toxic Apples (value ≤ -2)
```
Distance 2:             -1000 (emergency avoidance)
Distance 3-10:    -50 × (10 - distance)
Bait opportunity:        +200
```

#### Fresh Apples (value > 3)
```
Base score:        value × 40
Distance penalty:    -3 × distance
Adjacent (dist 1):       +200
Path blocking:           +150
```

#### Moderate Apples (value 1-3)
```
Base score (if safe):  value × 20
Distance penalty:    -4 × distance
```

### **5. Advanced Combat**

#### Headshot Setup (size diff > 2, dist ≤ 3)
```
Base:                    +200
Size bonus:        +30 × sizeDiff
```

#### Corner Trap (size diff > 1, dist ≤ 4)
```
Cornered enemy:          +100
Distance adjustment: -20 × distance
```

#### Suicide Bait (toxic apple nearby)
```
Successful positioning:  +200
```

### **6. Psychological Factors**

#### TURTLE Mode (length < 10)
```
Per reachable space:     +10
Enemy within 3:         -100
```

#### CALCULATED_COWARD (2+ stronger opponents)
```
Per distance from threat: +15
```

#### OPPORTUNIST (default)
```
Weakened target:         +150
Distance penalty:    -20 × distance
```

#### PREDATOR (kills ≥ 2)
```
Hunting weaker snake: +25 × (8 - distance)
```

#### Decoy Dance (length 12-25, turn % 3 = 0)
```
Non-straight movement:   +20
```

### **7. Space Denial** (length > 20)
```
Per controlled quadrant: +15
Access blocking:        +100
```

---

## 🔧 Original System Scores (Preserved)

### **Safety Checks**
```
Reachable space < 3:   -5000 (death trap)
Reachable space < length: -1000 (potential trap)
Per reachable space:       +8
```

### **BFS Path Quality**
```
Has valid path:          +100
Path quality:     50 - (2 × pathLength)
No path available:       -150
```

### **Enemy Assessment**
```
Larger enemy, dist ≤ 2:  -500
Smaller enemy, dist ≤ 3: +100
```

### **Wall Safety**
```
Per wall distance unit:   +3
```

---

## 🎮 Scoring Examples

### **Example 1: Fresh Apple (value 5), Safe Position**
```
Apple Risk/Reward:  5 × 40 - 3 × 2 = +194
Path blocking:                      +150
Space (20 tiles):   20 × 8         +160
BFS path (5 steps): 50 - 2×5       +40
Wall safety (2 units): 2 × 3       +6
----------------------------------------
TOTAL:                              +550
```

### **Example 2: Toxic Apple (value -3), Bait Setup**
```
Apple avoidance:  -50 × (10-5)    -250
Bait positioning:                  +200
Space (15 tiles):  15 × 8          +120
Wall hugging (2 units):            +25
----------------------------------------
TOTAL:                              +95
(Negative apple avoided, but bait opportunity)
```

### **Example 3: Combat Intercept (PREDATOR mode)**
```
Headshot setup:                    +200
Size advantage (5):  30 × 5        +150
Predator bonus:  25 × (8-2)        +150
Space (12 tiles):  12 × 8          +96
Corner trap:                       +100
----------------------------------------
TOTAL:                              +696
(Highly aggressive combat positioning)
```

### **Example 4: Defensive Retreat (CALCULATED_COWARD)**
```
Distance from threat1:  15 × 4     +60
Distance from threat2:  15 × 5     +75
Wall hugging:                      +25
Space (18 tiles):  18 × 8          +144
Body guard (3 protected):  3×8     +24
----------------------------------------
TOTAL:                              +328
(Safe defensive positioning)
```

### **Example 5: Territory Control (Length 25)**
```
Space denial (3 quadrants): 3×15   +45
Access blocking:                   +100
Body guard (4 protected):  4×8     +32
Space (22 tiles):  22 × 8          +176
Spiral pattern:                    +15
----------------------------------------
TOTAL:                              +368
(Late-game dominance)
```

---

## ⚠️ Critical Penalties

### **Immediate Death Traps**
```
Reachable space < 3:     -5000
Head-to-head loss:       -3000
Toxic apple adjacent:    -1000
```

### **Dangerous Situations**
```
Reachable space < length: -1000
Larger enemy dist ≤ 2:     -500
Enemy dist ≤ 2 (equal):    -300
```

---

## 🎯 Decision Priority Flow

```
1. Emergency Escape:    Score irrelevant, MUST move
2. BFS to Apple:        Direct path if score > 0
3. Hamiltonian:         Safe fallback if validated
4. Best Scored Move:    Highest total from all strategies
5. Relative Moves:      Last resort (moves 4-6)
6. Any Safe Move:       Ultimate fallback to prevent death
```

---

## 📈 Score Thresholds

### **Excellent Moves** (500+)
- Fresh apple with clear path
- Combat advantage with safety
- Territory control positioning

### **Good Moves** (200-499)
- Moderate apples
- Defensive positioning
- Space preservation

### **Acceptable Moves** (0-199)
- Safe but not optimal
- Waiting/positioning moves
- Conservative plays

### **Risky Moves** (-199 to -1)
- Slightly toxic apples
- Crowded areas
- Reduced space

### **Dangerous Moves** (-200 to -999)
- High collision risk
- Toxic apples nearby
- Trap potential

### **Death Traps** (-1000+)
- Immediate collision
- No escape routes
- Guaranteed loss

---

## 🔄 Dynamic Adjustments

### **Personality Modifiers**

| Personality | Apple Priority | Combat Priority | Space Priority |
|------------|---------------|----------------|---------------|
| TURTLE | Low (-50%) | None (0%) | High (+100%) |
| CALCULATED_COWARD | Moderate | Avoid (-100%) | High (+50%) |
| OPPORTUNIST | High | Selective | Moderate |
| PREDATOR | Moderate | High (+200%) | Moderate |

### **Game Phase Modifiers**

| Phase | Aggression | Safety | Territory |
|-------|-----------|--------|-----------|
| Early (len <10) | -50% | +100% | +0% |
| Mid (len 10-25) | +0% | +0% | +50% |
| Late (len 26+) | +50% | -25% | +150% |

---

## 🏆 Optimal Score Ranges by Scenario

### **Apple Competition**
- **Winning Position**: 400-600
- **Competitive**: 200-399
- **Losing**: < 200

### **Combat Engagement**
- **Dominant**: 500-800
- **Favorable**: 300-499
- **Retreat**: < 300

### **Survival Mode**
- **Safe**: 200-400
- **Risky**: 100-199
- **Desperate**: < 100

---

This scoring reference helps understand why the AI makes specific decisions and how different strategies contribute to move selection.
