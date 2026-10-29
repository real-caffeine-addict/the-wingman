<div align="center">

# Sammy · The Wingman

**A personal assistant that lives where the conversation already happens: WhatsApp.**

Java 25 · Spring Boot · Cobalt · Under active development

*First, make it useful. Then, make it clever.*

</div>

---

## What's the idea?

Sammy is the bot behind **Wingman**: a general-purpose personal assistant operating through a dedicated WhatsApp account, in personal chats and groups.

The first version has a deliberately small job: recognize when someone is talking to it, handle predefined questions and answers, and persist the information it needs. No LLM required. A greeting should not need a GPU and a philosophical debate.

Over time, Sammy may gain contextual memory, skills for specific conversations, external integrations, and AI-powered assistance. Those are directions to explore, not features hiding behind an “almost done” label.

## What we're building toward

- **A useful WhatsApp assistant** — available in personal conversations and groups, with clear rules for when to respond.
- **A dependable foundation** — connection handling, message processing, and persistence that can be understood and maintained.
- **Conversation-specific capabilities** — eventually, different chats can use different skills, such as travel assistance or media management.
- **Practical engineering experience** — building the product while improving testing, Git workflows, code review, and, later, CI/CD and deployment.

The goal is working software and stronger engineering habits. Collecting design patterns is not a side quest.

## Where things stand

**Early development: the connection works; the first chatbot is taking shape.**

The following status reflects development checks reported as of **29 October 2026**. Features under development are listed separately from verified behavior.

| Area | Status | What's there |
| :--- | :--- | :--- |
| Application foundation | Working | Java 25, Spring Boot, and a Maven Wrapper build |
| WhatsApp connection | Verified manually | A dedicated account connects through Cobalt |
| Session reuse | Verified manually | An existing session is loaded without scanning a new QR code |
| Reconnection | Verified manually | Disconnecting and restoring the network resulted in reconnection |
| Shared connection management | Established | `ConnectionManager` initializes the connection for the application's bot features |
| Application lifetime | Configured | Spring's keep-alive setting keeps the application running |
| Echo prototype | Verified manually | Incoming personal-chat messages can be printed and answered |
| First deterministic chatbot | In progress | Message handling and initial tests are being developed |
| Group mentions and replies | Planned | Respond when addressed in a group |
| Predefined Q&A and persistence | Planned | The core of the first useful version |
| AI, external integrations, and contextual memory | Future possibilities | No implementation commitment yet |

Session reuse means reusing the WhatsApp login session. It does **not** mean conversation data or user answers are already persisted.

The reconnection check confirms the scenario tested so far; broader recovery behavior still needs to be exercised as the application develops.

## The first useful version

The immediate target is intentionally modest:

1. Receive a message and identify the conversation and sender.
2. Decide whether Sammy should respond: a direct personal message or a mention in a group.
3. Reply with a simple greeting, such as “Hi [name], how are you?”
4. Extend that flow into predefined questions and answers.
5. Persist the relevant answers and state.

Group handling and the greeting flow are targets, not a claim that the complete flow is already implemented.

## Under the hood

| Component | Technology |
| :--- | :--- |
| Language | Java 25 |
| Application framework | Spring Boot |
| WhatsApp connectivity | Cobalt — unofficial WhatsApp client library |
| Build | Maven Wrapper |
| Persistence | Not implemented yet; technology to be decided |

The current foundation uses a Spring-managed `ConnectionManager` to initialize a linked WhatsApp web client when the application is ready. It loads the latest saved connection or creates one, and supports terminal QR pairing when registration is needed.

That connection is shared infrastructure for Sammy's features. The echo experiment is simply the first consumer, not the owner of the connection.

Dependency versions and the pinned Cobalt revision belong in `pom.xml`, which remains the source of truth.

## Run locally

You'll need **JDK 25**, internet access, and access to the dedicated WhatsApp account for initial pairing. Run these commands from the repository root.

Build and run the current tests:

```bash
./mvnw verify
```

Start Sammy:

```bash
./mvnw spring-boot:run
```

If the account needs pairing, scan the QR code printed in the terminal using WhatsApp's **Linked devices** flow. When a usable saved session exists, startup can proceed without a new QR code.

The connection listener currently prints `Sammy connected` after login.

**Keep session data out of Git.** Saved connection credentials are account access material, not project documentation.

## How this project grows

- Start with a small flow that works end to end, then extend it.
- Keep WhatsApp connection handling separate from bot behavior.
- Use straightforward Java and framework capabilities before adding custom machinery.
- Add abstractions when they solve an actual problem. Future Sammy can submit a feature request like everyone else.
- Test meaningful behavior as it is introduced. CI/CD comes when there is a useful workflow to automate.
- Make changes in focused commits and use reviews to catch problems while they are still small.

Architecture, persistence, and deployment choices remain open until the requirements justify a decision.

---

<div align="center">

**Less ceremony. Better engineering. A wingman that actually answers.**

</div>
