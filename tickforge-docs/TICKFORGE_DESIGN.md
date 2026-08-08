# Tickforge Design Document

**Status:** Target architecture  
**Date:** 8 August 2026  
**Repository:** `WellseyInSpaceAgain/tickforge`  
**Development branch:** `tickforge-main`

---

## 1. Purpose

Tickforge is a modular RuneLite framework for building, testing, inspecting, recording, and executing tick-driven OSRS activities.

The project should support two complementary approaches to automation:

1. **Traditional activity modules** written deliberately against RuneLite game state and semantic interactions.
2. **Recorded-data tooling** that observes real player behaviour and uses those recordings to assist with generation of mouse behaviour and complete activity scripts.

These approaches should share the same core runtime rather than becoming separate automation systems.

Tickforge should make it easy to:

- inspect what RuneLite knows about the current game state;
- inspect what interactions manual player actions produce;
- execute semantic interactions through one consistent interaction layer;
- write small tick-driven activity state machines;
- determine whether issued actions actually succeeded;
- record real mouse movement and input behaviour;
- record complete manually performed game activities;
- transform recordings into reusable automation behaviour;
- generate activity-module skeletons from recordings;
- test and debug all of the above from the Tickforge sidebar.

The framework should favour **observable, inspectable behaviour** over hidden magic.

Tickforge is primarily a development and experimentation framework. It is not intended to become a ready-to-run collection of finished automation activities that any random user can simply download and use without doing any work themselves.

---

## 2. Current Foundation

Tickforge is a RuneLite fork.

The repository maintains:

```text
master
    Clean/upstream RuneLite

tickforge-main
    Tickforge development
```

The current high-level structure is:

```text
tickforge-api/
└── src/main/java/dev/tickforge/api/
    └── module/
        └── TickforgeModule.java

runelite-client/
└── src/main/java/net/runelite/client/plugins/tickforge/
    ├── TickforgePlugin.java
    ├── framework/
    │   ├── ModuleRegistry.java
    │   └── TickforgeModuleManager.java
    ├── activities/
    │   └── ...
    ├── devtools/
    │   └── ...
    └── ui/
        └── TickforgePanel.java
```

Do not introduce competing `module/` and `modules/` package hierarchies.

The existing meanings should remain:

- `framework/` — runtime, lifecycle, execution, and reusable infrastructure.
- `activities/` — actual automated game activities.
- `devtools/` — inspection, recording, probing, and generation tools.
- `ui/` — Tickforge RuneLite sidebar and related UI.

Supporting packages beneath these are fine when features become sufficiently large.

The existing vertical slice has already proven the basic flow:

```text
RuneLite discovers TickforgePlugin
    ↓
Tickforge plugin starts
    ↓
TickforgeModuleManager constructs ModuleRegistry
    ↓
modules are registered
    ↓
sidebar lists registered modules
    ↓
Start/Stop controls invoke module lifecycle
    ↓
stopping Tickforge stops all running modules
```

This infrastructure should be treated as settled unless a concrete implementation problem requires a change.

---

## 3. Core Design Principles

### 3.1 Tickforge is tick-driven

OSRS game state progresses around game ticks, so activity decision making should primarily happen in response to game state/events rather than arbitrary polling loops.

An activity should broadly behave as:

```text
observe
    ↓
decide
    ↓
issue semantic interaction
    ↓
wait
    ↓
observe outcome
    ↓
continue / recover
```

Activities should not contain long blocking loops or sleep-driven scripts.

Real-time input facilities such as mouse movement may operate at finer timing resolution, but game-level decision making remains tick/event driven.

---

### 3.2 Separate perception, decision, and execution

An activity should answer:

> What should happen next?

It should not need to know all of the mechanics required to make that interaction happen.

Conceptually:

```text
RuneLite state/events
        ↓
    Perception
        ↓
     Activity
      Decision
        ↓
InteractionRequest
        ↓
InteractionExecutor
        ↓
 Input / menu action
        ↓
 Outcome observation
        ↓
InteractionResult
```

This separation is fundamental to Tickforge.

---

### 3.3 Activities should use semantic actions

Prefer:

```text
interact with this NPC using "Attack"
interact with this object using "Climb"
click this widget action
use this inventory item on this object
walk to this destination
```

over:

```text
click coordinate 712, 418
call client.menuAction(...) directly
set some arbitrary client state
```

Coordinates, menu identifiers, and low-level input details are implementation concerns underneath the semantic interaction layer.

---

### 3.4 One interaction execution path

Gameplay activity modules must not independently invent ways of performing interactions.

There should be a shared interaction executor responsible for:

- resolving targets;
- resolving menu actions;
- validating requests;
- marshalling operations onto the appropriate RuneLite/client thread;
- issuing required input events;
- invoking the actual game interaction;
- recording interaction metadata;
- tracking an issued request;
- observing success/failure;
- applying timeouts;
- returning an outcome.

This is also the point where future input implementations can be changed without rewriting every activity.

---

### 3.5 Developer tooling is a first-class feature

Tickforge should optimise for:

> **providing the tools to build automation, not providing every finished automation.**

Developer tooling is therefore not merely internal debugging infrastructure.

The intended workflow should make it straightforward for a developer — including one working with an LLM or coding agent — to inspect the client, understand an activity, collect local data, implement behaviour, and refine it.

The desired experience is closer to:

```text
"I want Tickforge to perform X."

→ inspect or record X
→ use the resulting information in the development workflow
→ implement or generate the activity
→ run it
→ inspect failures
→ improve it
```

than:

```text
"I want Tickforge to perform X."

→ download an existing finished script
→ press Start
```

---

## 4. Module Model

The existing `TickforgeModule` abstraction remains the top-level lifecycle unit.

Conceptually:

```java
public interface TickforgeModule
{
    String getId();
    String getName();

    default String getDescription()
    {
        return "";
    }

    void startUp();
    void shutDown();
}
```

`ModuleRegistry` owns whether a module is currently running.

Individual modules should not duplicate this with their own `enabled` flag unless they have genuinely distinct internal operational state.

Modules are explicitly registered through `TickforgeModuleManager`.

Do not create a custom Guice discovery system merely to avoid maintaining the registration list.

Explicit registration makes the active Tickforge feature set easy to inspect.

---

## 5. Module Categories

Tickforge modules currently fall into three conceptual groups.

### 5.1 Developer tools

Passive or developer-directed tooling.

Examples:

```text
Menu Entry Snapshot
Interaction Probe
Game State Snapshot
Event Trace
Mouse Recorder
Activity Recorder
Script Generator
```

These exist primarily to understand RuneLite, collect data, debug behaviour, or build new automation.

---

### 5.2 Activity modules

Executable game activities.

Examples might eventually include:

```text
Agility
Mining
Woodcutting
Banking sequences
Combat activities
Quest/activity fragments
```

An activity module should be small enough that its behaviour can be reasoned about and tested independently.

Activities should consume reusable framework services instead of duplicating interaction, recording, or state-management infrastructure.

---

### 5.3 Framework services

These are not user-selectable activities.

Examples:

```text
InteractionExecutor
TargetResolver
InteractionTracker
ProgressWatchdog
MouseInputService
RecordingService
SessionStorage
AccessService
```

They support modules and should normally live under `framework/` or an appropriate child package.

---

## 6. Initial Developer Tooling

The first developer-tool set remains important because later automation depends upon reliable observation.

### 6.1 Menu Entry Snapshot

Observe the current RuneLite menu entries.

Capture information such as:

- option;
- target;
- action type;
- identifier;
- `param0`;
- `param1`;
- item ID;
- world-view ID.

Prefer change-based logging rather than dumping identical menu state continuously.

---

### 6.2 Interaction Probe

Observe interactions generated through normal manual gameplay.

The tool should listen to RuneLite interaction events such as `MenuOptionClicked` and record the resulting `MenuEntry`.

It must not modify or consume the player's interaction.

This provides a direct answer to:

> What does RuneLite think I just clicked?

This becomes particularly important for Activity Recording.

---

### 6.3 Game State Snapshot

Capture compact player/game state on game ticks.

Useful initial state includes:

- game state;
- world;
- player location;
- plane;
- animation;
- current interaction target;
- selected item/spell where relevant;
- other state required by later activity tooling.

Only log snapshots when they change where practical.

Use current RuneLite APIs and events rather than recreating a manual hooking layer for information RuneLite already exposes.

---

### 6.4 Event Trace

Maintain a bounded recent trace of important RuneLite events.

Initial examples:

- `GameStateChanged`;
- `AnimationChanged`;
- `InteractingChanged`;
- relevant inventory/container changes;
- relevant varbit/varp changes where useful.

Do not indiscriminately record every EventBus event.

The trace exists to answer questions such as:

> What changed after I clicked that?

---

## 7. Interaction Architecture

The interaction system is the most important shared component of executable Tickforge activities.

### 7.1 Interaction Request

Activities should describe what they want performed through an `InteractionRequest` or equivalent abstraction.

A request should represent concepts such as:

```text
target
desired action
optional constraints
expected result
timeout/retry policy where appropriate
```

The API should avoid requiring activities to construct raw RuneLite menu parameters unnecessarily.

For example:

```text
NPC: Banker
Action: Bank

Object: Tree
Action: Chop down

Widget: Inventory item
Action: Eat
```

The implementation may resolve this into RuneLite `MenuEntry`/menu-action values underneath.

---

## 8. Default Gameplay Interaction Execution

Direct standalone calls to:

```java
client.menuAction(...)
```

must not be the normal gameplay execution path.

The current Tickforge working assumption is that gameplay menu actions should be accompanied by an input event.

The default semantic interaction sequence should therefore be conceptually:

```text
InteractionRequest
        ↓
resolve target/action
        ↓
construct interaction
        ↓
emit synthetic AWT LEFT click
        ↓
immediately execute menu action
        ↓
track expected result
```

The synthetic event must specifically represent a **left mouse click**.

The current preferred implementation may use sentinel coordinates such as:

```text
(-1, -1)
```

where appropriate.

The precise AWT dispatch implementation should remain centralized so it can be tested and changed without touching activity modules.

### Important

Do **not** add artificial mouse movement merely because an interaction contains a synthetic click.

Current default behaviour is:

```text
synthetic left click
+
menu action
```

not:

```text
invent fake mouse path
+
click
+
menu action
```

Poor synthetic mouse movement is less desirable than no mouse movement.

The separate mouse-generation system described later exists so that real movement behaviour can be studied before it is used.

---

## 9. Interaction Backends

Activities should not be permanently coupled to one low-level execution mechanism.

The architecture should permit multiple interaction backends while keeping the semantic activity API stable.

Conceptually:

```text
                InteractionRequest
                       |
                InteractionExecutor
                       |
            +----------+----------+
            |                     |
 SyntheticClickMenuAction     InputBackend
            |                     |
 AWT left click             mouse movement
 + menuAction               real click/input path
```

The synthetic-click + menu-action backend is the normal initial gameplay backend.

A fuller input backend can later use generated mouse movement and conventional input events.

This allows experimentation without rewriting activities.

---

## 10. Interaction Outcomes

Issuing an interaction is not equivalent to successfully completing one.

Tickforge should model the lifecycle of an action.

Useful result states include equivalents of:

```text
ISSUED
CONFIRMED
FAILED
TIMED_OUT
TARGET_NOT_FOUND
CANCELLED
```

Activities should normally wait for meaningful evidence of progress instead of assuming success immediately after issuing input.

For example:

```text
Chop tree
    ↓
player animation starts
    ↓
CONFIRMED
```

or:

```text
Open bank
    ↓
bank widget becomes visible
    ↓
CONFIRMED
```

The success condition depends on the semantic action.

This is important for resilient scripts and especially important for generated scripts.

---

## 11. Progress and Stall Detection

Tickforge should distinguish between:

- issuing input;
- the game actually progressing.

A shared progress watchdog should eventually be capable of determining that an activity has stalled.

Examples:

```text
interaction issued but expected state never appeared
player unexpectedly stopped
target disappeared
movement did not begin
inventory did not change
activity remained in one state too long
```

The watchdog should report or trigger activity recovery.

It should **not** blindly perform random actions just because a timer elapsed.

Recovery remains an activity-level decision.

---

## 12. Mouse Recording System

Mouse recording is a developer tool and data-collection facility.

Its purpose is not simply to replay one recorded path.

Its purpose is to build a dataset describing how real mouse movement behaves so Tickforge can later generate new paths from observed behaviour.

### 12.1 MouseRecorderModule

A selectable Tickforge developer module should record manual mouse activity while the player uses RuneLite normally.

Useful data includes:

```text
timestamp
elapsed time from previous event
x
y
event type
button
press/release/click information
wheel activity if useful
whether pointer is inside client canvas
client/canvas dimensions
```

Where possible, use coordinates relative to the RuneLite game canvas rather than assuming fixed desktop coordinates.

Recording should have explicit:

```text
Start Recording
Stop Recording
Save Session
Discard Session
```

controls.

---

## 13. Mouse Movement Dataset

A recording should preserve the original event stream rather than immediately reducing it to a simplistic representation.

From that raw data Tickforge can later derive:

- movement duration;
- start/end displacement;
- total path length;
- velocity over time;
- acceleration/deceleration;
- curvature;
- overshoot;
- corrections;
- pauses;
- movement immediately preceding clicks.

Sessions should include enough metadata to understand their coordinate system.

The data format should be versioned.

A simple JSON/JSONL representation is adequate initially; the important requirement is that recordings can be replayed, analysed, and migrated.

---

## 14. Mouse Movement Generation

`MouseMovementGenerator` should be distinct from the recorder.

Its job is:

```text
start point
+
target point/region
+
recorded human movement data
        ↓
generated trajectory
```

The first implementation does not require machine learning.

A sensible progression is:

### Stage 1 — Recorded trajectory library

Take real recorded movements and:

- classify them by approximate displacement/direction/duration;
- normalize their coordinate space;
- select an appropriate recorded movement;
- transform it for the new start/end points.

### Stage 2 — Statistical variation

Model properties observed in the recording corpus:

- duration;
- curvature;
- velocity profile;
- number and size of corrective movements;
- pauses;
- endpoint behaviour.

Generate new trajectories following those distributions.

### Stage 3 — More sophisticated model

Only if the dataset justifies it, investigate a learned movement model.

The architecture should not require this stage.

The most important property is that generated behaviour comes from measured recordings rather than arbitrary "human-like" constants invented in code.

---

## 15. Mouse Movement Is Optional for Semantic Actions

Mouse generation should not become a mandatory dependency of every Tickforge interaction.

There are two legitimate paths:

```text
Semantic interaction
→ synthetic left click
→ menuAction
```

and later:

```text
Semantic interaction
→ resolve physical target
→ generated mouse path
→ real click/input path
```

This distinction lets Tickforge experiment with input approaches without coupling the activity layer to either one.

---

## 16. Activity Recording

Mouse recording alone captures motion but lacks semantic meaning.

The **Activity Recorder** should record a richer session while the player manually performs an in-game activity.

Its purpose is to answer:

> What did the player do, what did they interact with, what state were they in, and what changed as a result?

---

## 17. ActivityRecorderModule

The activity recorder should combine multiple existing Tickforge observation sources.

Conceptually:

```text
AWT mouse/input events
        +
MenuOptionClicked
        +
MenuEntry information
        +
game tick
        +
player/game snapshots
        +
important EventBus events
        ↓
Activity Recording
```

The session should therefore contain both raw behaviour and semantic behaviour.

Example:

```text
Tick 18422

Mouse movement:
    (521, 381)
    ...
    (603, 412)

Mouse:
    left click

RuneLite interaction:
    option = "Chop down"
    target = "Tree"
    identifier = ...
    action = GAME_OBJECT_FIRST_OPTION
    world location = ...

State before:
    player idle
    inventory = ...

State after:
    player animation = woodcutting animation
    interacting object = tree
```

This is dramatically more useful than simply recording screen coordinates.

---

## 18. Semantic Recording

The recorder should attempt to preserve identities that generalize beyond one session.

For clicked entities capture data such as:

### NPC

```text
NPC ID
name
available actions
world location
relative position
selected menu action
```

### Game object

```text
object ID
name
world location
plane
available actions
selected action
```

### Inventory item

```text
item ID
name
slot
selected action
quantity where relevant
```

### Widget

```text
widget ID
child/index where appropriate
action
relevant item ID
```

Screen coordinates remain useful for mouse analysis but should not be the primary identity of a game interaction.

---

## 19. Activity Recording Sessions

The user should be able to label recordings.

Example:

```text
Activity: "Draynor oak woodcutting"
Run: 001
Notes: "bank when inventory full"
```

Multiple recordings of the same activity should be supported.

Repeated recordings are particularly valuable because they let later tooling distinguish:

```text
what always happens
```

from:

```text
what merely happened once
```

This is essential for generating useful scripts rather than brittle recorded macros.

---

## 20. Action Segmentation

Raw activity recordings should eventually be transformed into semantic action sequences.

For example, many low-level events:

```text
mouse move
mouse move
mouse move
mouse click
MenuOptionClicked
AnimationChanged
inventory changed
```

may become:

```text
Action 12:
    interact TREE using CHOP_DOWN

    precondition:
        player idle

    confirmation:
        woodcutting animation begins

    result:
        inventory eventually gains logs
```

The exact inference system can evolve.

The important design requirement is to preserve the raw recording so segmentation can be improved later without recollecting the original data.

---

## 21. Script Generator

The Script Generator is a **separate Tickforge module/tool**.

It should not be embedded inside Activity Recorder.

The recorder's responsibility is:

> collect accurate data.

The generator's responsibility is:

> turn recorded data into an automation implementation.

This separation should be maintained even if the two tools share supporting classes.

---

## 22. Initial Script-Generation Goal

The initial goal should not be:

> Watch something once and magically produce a perfect autonomous bot.

The first useful target is:

> Turn one or more recordings into a readable Tickforge activity-module skeleton containing the observed states, targets, semantic interactions, and likely success conditions.

For example, recordings of:

```text
chop trees
wait
inventory full
walk to bank
bank logs
return
```

might generate a state structure conceptually similar to:

```text
IF inventory full:
    bank

ELSE IF player at bank:
    return to trees

ELSE IF currently woodcutting:
    wait

ELSE:
    find suitable tree
    chop tree
```

The generated activity then uses normal Tickforge APIs.

It must not contain its own bespoke input system.

---

## 23. Generated Activity Structure

Generated code should prefer readable state-driven logic.

Each generated step should ideally expose:

```text
entry/precondition
action
expected outcome
timeout
recovery/next evaluation
```

A generated module must be understandable and editable by a developer.

Generation quality is more important than producing large amounts of code.

The system should avoid hardcoding recorded screen coordinates when semantic RuneLite information is available.

---

## 24. Multi-Recording Generalization

The strongest form of the generator will compare multiple recordings of the same activity.

Suppose three sessions contain:

```text
Tree ID 10820 at tile A
Tree ID 10820 at tile B
Tree ID 10820 at tile C
```

The generator should eventually infer something closer to:

```text
find a suitable nearby tree with ID 10820
```

rather than:

```text
click the exact tree at tile A
```

Likewise, if different recordings use different bank booths but the same semantic action, the generator should have enough information to generalize appropriately.

This is one of the main reasons activity recordings must contain game semantics as well as mouse data.

---

## 25. Relationship Between Recording Systems

The intended architecture is:

```text
                   MANUAL PLAY
                       |
          +------------+------------+
          |                         |
    Mouse Recorder             Activity Recorder
          |                         |
          |                  semantic actions
          |                  game state/events
          |                  mouse/click data
          |                         |
          v                         v
    Mouse Dataset          Activity Dataset
          |                         |
          v                         v
 Mouse Movement              Script Generator
    Generator                     |
          |                       v
          |                 Activity Module
          |                       |
          +-----------+-----------+
                      |
               Tickforge Runtime
                      |
              InteractionExecutor
```

These systems complement rather than replace traditional manually written activities.

---

## 26. Local-Only Recorded Data

All behavioural data collected by Tickforge must be treated as **local user data**.

This includes, but is not limited to:

- mouse movement recordings;
- mouse click recordings;
- Activity Recorder sessions;
- semantic interaction traces;
- generated movement datasets;
- analysed recording data;
- activity-generation intermediate data;
- locally generated behavioural profiles.

None of this data should be committed to the Tickforge repository.

The repository may contain:

```text
schemas
example structures using synthetic/fake data
recording readers/writers
analysis algorithms
generation algorithms
documentation
```

but it should not contain real recorded gameplay behaviour.

Tickforge should provide an explicit local data directory, conceptually:

```text
tickforge-local/
├── recordings/
│   ├── mouse/
│   └── activities/
├── datasets/
├── generated/
└── analysis/
```

The exact physical location should be chosen appropriately for the RuneLite/Tickforge environment and does not need to live inside the Git working tree.

If development convenience requires a local directory beneath the repository, the entire directory must be excluded through `.gitignore`.

Automated tests must use synthetic fixtures rather than copying real user recordings into source control.

### 26.1 No bundled behavioural dataset

Tickforge should **not ship with a ready-made behavioural dataset**.

In particular, the repository should not contain a corpus of recorded mouse movements intended to make generated input immediately usable.

A Tickforge user who wants recording-derived behaviour should create their own dataset by using the recording tools.

This is intentional.

The framework provides the tools:

```text
record
→ inspect
→ analyse
→ generate
→ test
→ refine
```

It does not provide the finished behavioural profile.

### 26.2 No turnkey activity library requirement

Tickforge is primarily a development and experimentation framework rather than a ready-to-run collection of finished automation activities.

The repository may contain small reference or proof activities where they are useful for demonstrating framework behaviour, but the project's objective is not:

> clone repository → select activity → immediately run a complete prebuilt automation suite

Instead, the intended workflow is:

```text
clone Tickforge
      ↓
understand the framework
      ↓
use the developer tools
      ↓
collect local observations/recordings
      ↓
write or generate activities
      ↓
inspect and refine them
```

Users are expected to do development work themselves.

This philosophy should influence implementation decisions: prefer exposing powerful, understandable primitives over hiding the process behind one-click automation.

### 26.3 Generated code vs recorded data

Generated activity source code may be placed into the repository **only through an explicit developer action**.

The distinction is:

```text
Recorded behavioural data
    → always local-only

Generated/intermediate analysis files
    → local-only by default

Generated Java activity source
    → may be deliberately promoted into source control by the developer
```

The Script Generator should therefore generate into a local staging/output area first rather than silently inserting generated activities into tracked source directories.

The developer can then inspect, edit, and intentionally promote useful generated code into `activities/`.

---

## 27. Lightweight Client Access Gate

Tickforge is not intended to be distributed as a freely usable, ready-to-run botting client.

Because automation tooling is currently under significant scrutiny, the project should include a simple access check that demonstrates this intent.

The objective is **not** to build sophisticated DRM, anti-tamper protection, or an unbreakable licensing system.

A simple remote entitlement check is sufficient.

Conceptually:

```text
Tickforge starts
      ↓
AccessService
      ↓
validate configured credential
      ↓
authorized?
   ↙       ↘
 yes        no
 ↓           ↓
continue   Tickforge capabilities unavailable
```

The credential could initially be something simple such as an API key configured locally.

The implementation should:

- keep the access-check logic centralized;
- avoid scattering authorization checks throughout individual activity modules;
- fail closed when the authorization service explicitly rejects access;
- handle temporary network/service failures sensibly during development;
- never commit private credentials to the repository;
- load credentials from local configuration or environment;
- avoid logging complete credentials;
- make it straightforward to replace the mechanism later.

For example:

```text
framework/
└── access/
    ├── AccessService
    ├── AccessResult
    └── RemoteAccessService
```

The rest of Tickforge should consume a simple capability such as:

```java
boolean isAuthorized();
```

rather than knowing how authorization works.

### 27.1 Security expectations

This mechanism should be treated as a **usage gate and statement of project intent**, not as a strong security boundary.

Tickforge is a locally executing Java client built from an open-source-derived codebase. A sufficiently motivated developer with access to the code can inspect and modify it.

The project should therefore not waste substantial engineering effort attempting to make the check impossible to remove.

Do not introduce:

```text
heavy obfuscation
anti-debugging
complex integrity checks
native anti-tamper systems
continuous cat-and-mouse protection
```

unless there is a separate future requirement for them.

The access check exists primarily to ensure that the normal, supported build behaves as:

> Tickforge is usable by explicitly authorized developers, rather than functioning as a freely distributable turnkey automation client.

This also preserves the project's broader philosophy: Tickforge provides powerful development tooling, but it is not intended to remove every technical barrier for someone who simply downloads a build.

### 27.2 Distribution philosophy

Tickforge should distinguish between **source availability** and **supported access**.

Someone being technically capable of studying or modifying the project is not the same as Tickforge deliberately providing unrestricted ready-to-use automation.

The supported path should require:

```text
obtain/configure authorization
        ↓
run Tickforge
        ↓
use developer tooling
        ↓
collect local data
        ↓
build or generate activities
```

This provides a modest barrier to casual redistribution while keeping the implementation simple and understandable.

The project should not become preoccupied with defending this barrier from developers who intentionally modify their own local copy.

Engineering effort is better spent improving Tickforge's framework, observation tools, recording systems, activity-generation workflow, and reliability.

---

## 28. Storage

Recorded developer data should live outside activity source code.

A recording session should have:

```text
schema version
session ID
recording type
start/end times
canvas/client metadata
optional activity label
raw events
semantic events
game-state snapshots
```

A version field is mandatory so the format can evolve.

Avoid building a database initially unless actual data volume demonstrates a need for one.

Portable session files are preferable during early development because they are easy to inspect, diff locally, archive, and feed into external analysis tools.

The storage implementation must respect the local-only data policy described above.

---

## 29. UI Direction

The Tickforge sidebar remains the control surface.

The existing module list should continue to support basic lifecycle controls.

Individual modules may expose additional detail when selected.

Examples:

### Mouse Recorder

```text
[Start Recording]

Samples: 14,220
Duration: 01:42.3

[Stop] [Save] [Discard]
```

### Activity Recorder

```text
Activity label:
[Woodcutting - Draynor]

[Start Recording]

Ticks: 143
Interactions: 17
Mouse events: 8,412

[Stop] [Save]
```

### Script Generator

```text
Recordings:
[x] woodcutting-001
[x] woodcutting-002
[x] woodcutting-003

[Analyse]
[Generate Activity]
```

The UI should control modules, not construct or own their runtime dependencies.

---

## 30. Logging and Observability

Tickforge itself is a development framework, so its own behaviour must be observable.

Useful structured logging should exist for:

```text
module lifecycle
activity state transitions
interaction requests
resolved interactions
interaction dispatch
interaction outcomes
timeouts
recovery decisions
recording lifecycle
generation analysis
access-check results
```

Avoid continuously logging identical state.

Where possible use identifiers allowing one interaction to be followed through:

```text
request
→ dispatch
→ observed events
→ outcome
```

Credentials and sensitive local configuration must never be logged in full.

---

## 31. Threading

RuneLite/client state must be accessed on the appropriate thread.

The interaction layer should centralize client-thread marshaling rather than forcing every activity to solve threading independently.

Mouse trajectory playback, file serialization, remote access checks, and analysis should not unnecessarily block the client thread.

Game-state decisions should be based upon coherent RuneLite state snapshots.

---

## 32. Cancellation and Lifecycle

Stopping an activity must stop its outstanding work.

Stopping Tickforge must stop all running Tickforge modules.

Stopping a recording must unregister listeners and stop writing events.

Stopping an activity with an outstanding interaction must prevent that interaction from unexpectedly continuing later.

Repeated:

```text
start
stop
start
stop
```

cycles must not create duplicate EventBus subscriptions.

Lifecycle correctness is a framework requirement.

---

## 33. API Boundary

`tickforge-api` should contain contracts that are valuable independently from their RuneLite implementation.

Over time this may include contracts representing:

```text
TickforgeModule
InteractionRequest
InteractionResult
Activity state/progress
recording records/snapshots where appropriate
```

The RuneLite client should contain RuneLite-specific implementations such as:

```text
MenuEntry resolution
ClientThread integration
EventBus subscriptions
AWT input
client.menuAction
RuneLite sidebar UI
game-state extraction
```

Do not leak RuneLite implementation detail into the API simply to make the first implementation convenient.

At the same time, do not prematurely create abstractions for things Tickforge does not yet need.

---

## 34. Traditional Activity Design

A manually written activity should be intentionally boring.

A good activity mostly consists of state evaluation and semantic requests.

Conceptually:

```java
onGameTick()
{
    if (isFinished())
    {
        finish();
        return;
    }

    if (interactionExecutor.hasPendingInteraction())
    {
        return;
    }

    if (inventoryIsFull())
    {
        bank();
        return;
    }

    if (playerIsBusy())
    {
        return;
    }

    chopTree();
}
```

The real implementation need not look exactly like this.

The important point is that activities describe game logic while framework services handle interaction mechanics.

---

## 35. What Tickforge Should Not Become

Avoid turning Tickforge into:

### A giant monolithic bot script

Activities should remain independent modules.

### A coordinate macro recorder

Coordinates are useful data but RuneLite semantics are much richer.

### A collection of scripts each calling `menuAction` differently

Interaction execution belongs in one shared layer.

### A collection of arbitrary "human-like" random delays

Timing should be purposeful or derived from actual recorded behaviour where human-input modelling is desired.

### A reimplementation of RuneLite state

Use RuneLite APIs/EventBus wherever they already expose the information required.

### A machine-learning project before the basic framework works

Recorded-data approaches should begin with inspectable deterministic/statistical techniques.

More sophisticated models can be introduced when a dataset and measurable need exist.

### A turnkey public automation client

The repository should provide the framework and tools needed to build activities, not a complete prebuilt automation suite designed for casual use.

---

## 36. Development Order

Codex should choose individual implementation details, but dependencies broadly suggest this progression.

### Phase 1 — Observation foundation

Complete and validate:

```text
MenuEntrySnapshotModule
InteractionProbeModule
GameStateSnapshotModule
EventTraceModule
```

Goal:

> Tickforge can reliably tell us what the client knows and what manual player interactions produce.

### Phase 2 — Shared semantic interaction execution

Implement:

```text
InteractionRequest
target/action resolution
InteractionExecutor
synthetic AWT left-click dispatch
menuAction execution
basic outcome tracking
```

Goal:

> A minimal activity can request a semantic interaction without implementing its own input mechanics.

### Phase 3 — First real activity

Build one deliberately small activity end-to-end.

Its purpose is to validate:

```text
perception
decision
interaction execution
outcome confirmation
recovery
lifecycle
```

Do not choose the first activity based on impressive complexity.

Choose one that exercises the architecture clearly.

### Phase 4 — Mouse Recorder

Implement reliable manual mouse recording and session persistence.

Goal:

> Produce inspectable datasets from real play.

Do not build the mouse generator until recordings can be trusted.

### Phase 5 — Mouse Movement Generator

Use recordings to produce transformed/new movement trajectories.

Build visualization/debug tooling so generated paths can be compared against recorded paths.

Goal:

> Generated movement should be demonstrably based on recorded movement characteristics.

### Phase 6 — Activity Recorder

Correlate:

```text
mouse/input
menu interactions
game state
RuneLite events
game ticks
```

into complete labelled activity sessions.

Goal:

> A recording tells us both what the user physically did and what that action meant to RuneLite.

### Phase 7 — Script Generator

Analyse activity recordings and generate a first Tickforge activity skeleton.

Goal:

> A developer can perform a simple activity manually, record it, generate a module, inspect the generated logic, and refine it instead of writing the entire activity from zero.

### Phase 8 — Generalization

Use multiple recordings to improve generated target selection, conditions, state transitions, and recovery behaviour.

Only after this point should more sophisticated inference techniques become a priority.

### Phase 9 — Lightweight access gate

Add the centralized access check once the core development workflow is sufficiently stable that restricting supported use is useful.

Goal:

> The normal Tickforge build requires explicit authorization without turning access control into a major engineering project.

This phase may be moved earlier if distribution of working builds becomes a concern.

---

## 37. Architectural Acceptance Criteria

Tickforge's target architecture is working when the following are true:

1. Modules start and stop independently from the Tickforge sidebar.
2. Module lifecycle remains centrally owned by the registry.
3. Activities never need to call raw `client.menuAction(...)` directly.
4. The shared default gameplay interaction path emits the required synthetic AWT **left click** before its menu action.
5. Artificial mouse movement is not added to that path by default.
6. Interaction requests can be tracked through an observable outcome.
7. Activities are predominantly tick/event-driven state logic.
8. Client-thread handling is centralized appropriately.
9. Real mouse activity can be recorded into inspectable, versioned sessions.
10. Recorded behavioural data remains local-only and is excluded from source control.
11. The repository does not ship a ready-made behavioural recording corpus.
12. Mouse movement can be generated using characteristics derived from locally recorded data.
13. Complete manual activities can be recorded with both input data and RuneLite semantic context.
14. Activity recordings preserve raw source data even if higher-level actions are inferred.
15. Script generation exists independently from activity recording.
16. Generated activities consume the same Tickforge framework APIs as manually written activities.
17. Generated source is staged locally first and only enters the repository through explicit developer action.
18. Recorded screen coordinates are not used where a stable RuneLite semantic identity is available.
19. Multiple recordings can eventually be compared to generalize behaviour.
20. Progress failures and timeouts are observable rather than silently ignored.
21. Stopping an activity cancels its outstanding work cleanly.
22. Repeated module start/stop cycles leave no duplicate event subscriptions.
23. The supported Tickforge build can be placed behind a lightweight centralized authorization check.
24. The authorization system does not evolve into a large anti-tamper subsystem without a separate explicit requirement.
25. The architecture remains understandable enough that a developer can trace an activity from decision through input to observed result.

---

## 38. Guidance to Codex

Treat this document as the architectural direction, not as a demand to implement every proposed class or name literally.

Before modifying the project:

1. Inspect the existing Tickforge repository and current `tickforge-main` implementation.
2. Preserve working infrastructure unless there is a concrete reason to change it.
3. Compare the current implementation with this target design.
4. Break changes into small vertical slices that can be run and inspected.
5. Prefer proving one complete path over building broad unused abstractions.
6. Add tests around framework behaviour where useful.
7. Use `./run-tickforge.ps1` as the normal edit/build/launch path.
8. Use the real RuneLite client and its logs to validate integration behaviour.
9. Keep recording/generation infrastructure separate from executable activity logic.
10. Keep all gameplay interaction execution behind the shared executor.
11. Keep all recorded behavioural data local-only.
12. Never add real recording corpora to the repository.
13. Keep generated activity source local until the developer explicitly promotes it into the tracked source tree.
14. Treat the access gate as a lightweight usage control, not as DRM or an anti-tamper engineering project.
15. Prefer exposing useful development primitives over building a large bundled activity library.

When an architectural decision is unclear, prefer the solution that is:

```text
simpler
more observable
more reusable
easier to test
less coupled to one activity
```

The immediate objective is not maximum automation complexity.

The objective is to build a solid Tickforge foundation from which both **traditional activities** and **recording-driven automation** can be developed confidently.
