# Transformer Class

## Overview
The `Transformer` class is a Java implementation for transforming JSON objects representing p-plan in a knime workflows into ontology models using the Apache Jena framework.

## Dependencies
- Apache Jena: Framework for building Semantic Web and Linked Data applications.
- JSON.org: Toolkit for parsing and generating JSON.

## Features
- Converts knime workflow components (variables, steps, plans) into ontological entities.
- Handles file operations for reading and writing Turtle files.
- Manages namespaces for ontology.

## Usage
1. Initialize `Transformer` with a workflow JSON object.
2. Call `knimeToPplan()` to transform the JSON workflow into an ontology model.
3. Use `save()` to write the ontology model to a Turtle file.

```java
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) {
        JSONObject workflowJson = //... load your workflow JSON here
        Transformer transformer = new Transformer(workflowJson);
        transformer.knimeToPplan();
        transformer.save();
    }
}
```
## Note
1. p-plan.ttl should be in src/main/resources/.
2. Adaptation might be required for different JSON structures or ontologies.
3. Basic exception handling is implemented.
