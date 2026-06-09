# Стратегия автономного поиска (Koog Autoresearch Strategy)

Этот документ описывает архитектуру автономного ИИ-агента для генеалогического поиска, реализованного с использованием графовой стратегии библиотеки Koog 1.0.

Решение построено на основе графа (State Machine), который проходит через четыре последовательные фазы. Это позволяет жестко контролировать процесс поиска и избежать рандомного блуждания LLM по инструментам.

## Блок-схема графа состояний

Схема ниже визуализирует последовательность нод и переходов.

```mermaid
graph TD
    %% Настройки стилей
    classDef phase fill:#f9f2f4,stroke:#333,stroke-width:2px,color:#000;
    classDef startend fill:#d4edda,stroke:#28a745,stroke-width:2px,color:#000;
    classDef llm fill:#cce5ff,stroke:#004085,stroke-width:1px,color:#000;
    classDef tool fill:#fff3cd,stroke:#856404,stroke-width:1px,color:#000;
    classDef data fill:#e2e3e5,stroke:#383d41,stroke-width:1px,color:#000;

    Start(("Start")):::startend --> ScanPrompt

    %% ================= PHASE 1 =================
    subgraph Phase 1: SCAN
        direction TB
        ScanPrompt["ScanInstructions<br/>System Prompt"]:::data --> ScanReq
        ScanReq("Scanner<br/>LLM Request"):::llm
        
        ScanExec["ScanToolExecutor<br/>Tool Exec"]:::tool
        ScanFind[("ScanFindingsCollector<br/>Save Facts")]:::data
        ScanSend("ScanToolResultSender<br/>LLM evaluates tool results"):::llm
        
        ScanReq -- "if Tool Calls" --> ScanExec
        ScanExec --> ScanFind
        ScanFind --> ScanSend
        
        ScanSend -- "if MORE Tool Calls" --> ScanExec
        ScanSend -- "if Text Only" --> ScanReq
    end

    %% Условие выхода из SCAN
    ScanReq -- "Text Response<br/>(isSubstantiveResponse)" --> DiscPrompt

    %% ================= PHASE 2 =================
    subgraph Phase 2: DISCOVERY
        direction TB
        DiscPrompt["DiscoveryInstructions<br/>System Prompt"]:::data --> DiscReq
        DiscReq("Discovery<br/>LLM Request"):::llm
        
        DiscExec["DiscoveryToolExecutor"]:::tool
        DiscFind[("DiscoveryFindingsCollector")]:::data
        DiscSend("DiscoveryToolResultSender"):::llm
        
        DiscReq -- "if Tool Calls" --> DiscExec
        DiscExec --> DiscFind
        DiscFind --> DiscSend
        
        DiscSend -- "if MORE Tool Calls" --> DiscExec
        DiscSend -- "if Text Only" --> DiscReq
    end

    %% Условие выхода из DISCOVERY
    DiscReq -- "Research Plan" --> ResPrompt

    %% ================= PHASE 3 =================
    subgraph Phase 3: RESEARCH
        direction TB
        ResPrompt["ResearchInstructions<br/>System Prompt"]:::data --> ResReq
        ResReq("Research<br/>LLM Request"):::llm
        
        ResExec["ResearchToolExecutor"]:::tool
        ResFind[("ResearchFindingsCollector")]:::data
        ResSend("ResearchToolResultSender"):::llm
        
        ResReq -- "if Tool Calls" --> ResExec
        ResExec --> ResFind
        ResFind --> ResSend
        
        ResSend -- "if MORE Tool Calls" --> ResExec
        ResSend -- "if Text Only" --> ResReq
    end

    %% Условие выхода из RESEARCH
    ResReq -- "Research Summary" --> FinPrompt

    %% ================= PHASE 4 =================
    subgraph Phase 4: FINALIZE
        direction TB
        FinPrompt["FinalizeInstructions<br/>Injects all saved facts"]:::data --> FinReq
        FinReq("Finalizer<br/>LLM - Tools Disabled"):::llm
        Extractor["Extractor<br/>Extract string from obj"]:::data
        
        FinReq --> Extractor
    end

    Extractor --> Finish(("Finish")):::startend
```

### Описание фаз:

1. **Phase 1: SCAN**  
   Агент анализирует предоставленное дерево, выявляет регионы и запрашивает списки доступных архивных гайдов. Строго запрещено использовать основные поисковые инструменты на этой фазе.
2. **Phase 2: DISCOVERY**  
   Агент читает необходимые Markdown-руководства, собранные в фазе SCAN, изучает методологию и составляет подробный пошаговый план поиска.
3. **Phase 3: RESEARCH**  
   Основной процесс поиска. Агент использует инструменты взаимодействия с базами (например, Pamyat Naroda, FamilySearch), опираясь на выработанный план. Инструменты могут вызываться рекурсивно до тех пор, пока поиск не будет завершён.
4. **Phase 4: FINALIZE**  
   Агент собирает все факты (перехваченные и сохранённые специальными нодами `FindingsCollector` на предыдущих этапах) и формирует финальный сводный отчёт. Инструменты в этой фазе жестко отключены.
