import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDFS;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.apache.jena.ontology.OntModelSpec.OWL_MEM;

public class Transformer {

  private JSONObject jsonObject;
  private Map<String, String> output = new HashMap<>();
  private OntModel model = ModelFactory.createOntologyModel(OWL_MEM);

  OntClass entity;
  OntClass activity;
  OntClass step;
  OntClass variable;
  OntClass receiving;
  OntClass plan;
  OntClass system;
  DatatypeProperty type;

  ObjectProperty hasInputVar;
  ObjectProperty hasOutputVar;
  ObjectProperty isPreceededBy;
  ObjectProperty includesPlanElement;
  ObjectProperty hasServiceGenerator;
  ObjectProperty hasPlan;

  public Transformer(JSONObject jsonObject) {
    this.jsonObject = jsonObject;

    FileInputStream temp;
    try {
      temp = new FileInputStream("src/main/resources/p-plan.ttl");
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }

    RDFDataMgr.read(model, temp, Lang.TURTLE);

    this.entity = model.getOntClass("https://w3id.org/ep-plan#Entity");
    this.activity = model.getOntClass("https://w3id.org/ep-plan#Activity");
    this.step = model.getOntClass("http://purl.org/net/p-plan#Step");
    this.variable = model.getOntClass("http://purl.org/net/p-plan#Variable");
    this.receiving = model.getOntClass("http://w3id.org/wellfort/prov#Receiving");
    this.plan = model.getOntClass("http://www.w3.org/ns/prov#Plan");
    this.type = model.getDatatypeProperty("http://w3id.org/wellfort/prov#type");
    this.system = model.getOntClass("http://w3id.org/wellfort/prov#extension:System");
    this.hasInputVar = model.getObjectProperty("http://purl.org/net/p-plan#hasInputVar");
    this.hasOutputVar = model.getObjectProperty("http://purl.org/net/p-plan#hasOutputVar");
    this.isPreceededBy = model.getObjectProperty("http://purl.org/net/p-plan#isPrecededBy");
    this.includesPlanElement = model.getObjectProperty("https://w3id.org/ep-plan#includesPlanElement");
    this.hasServiceGenerator = model.getObjectProperty("http://w3id.org/wellfort/prov#hasServiceGenerator");
    this.hasPlan = model.getObjectProperty("http://w3id.org/wellfort/prov#hasPlan");
  }
  public void knimeToPplan() {
    JSONArray nodes = jsonObject.getJSONObject("workflow").getJSONArray("nodes");
    for (int i = 0; i < nodes.length(); i++) {
      JSONObject node = nodes.getJSONObject(i);
      if (node.getString("type").equals("Subnode")) {
        processSubnode(node);
      } else if (node.getString("name").equals("Variable") &&
              node.getJSONObject("factoryKey").getString("className").equals("org.pplan.variable.VariableNodeFactory")) {
        processVariableNode(node);
      } else if (node.getString("name").equals("Step") &&
              node.getJSONObject("factoryKey").getString("className").equals("org.pplan.step.StepNodeFactory")) {
        processStepNode(node);
      } else if (node.getString("name").equals("Plan") &&
              node.getJSONObject("factoryKey").getString("className").equals("org.pplan.plan.PlanNodeFactory")) {
        processPlanNode(node);
      }
    }
  }

  private void processPlanNode(JSONObject node) {

    String label = node.getString("annotation");
    Individual Iplan = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/plan#" + label);
    if (Iplan == null) {
      Iplan = plan.createIndividual("https://w3id.org/semsys/plan/ns/audit/plan#" + label);
    }

    Iplan.addProperty(RDFS.label, label);
    Individual Isystem = system.createIndividual("https://w3id.org/semsys/plan/ns/audit/system#"
            + node.getJSONArray("settings").getJSONObject(1).getString("value"));
    Isystem.addProperty(RDFS.label, node.getJSONArray("settings").getJSONObject(1).getString("value"));
    setOutput(node, Iplan);
  }
  private void processSubnode(JSONObject node) {
    JSONObject subWorkflow = node.getJSONObject("subWorkflow");
    JSONArray subNodes = subWorkflow.getJSONArray("nodes");
    for (int j = 0; j < subNodes.length(); j++) {
      JSONObject subNode = subNodes.getJSONObject(j);
      if (subNode.getString("name").equals("Variable") &&
              subNode.getJSONObject("factoryKey").getString("className").equals("org.pplan.variable.VariableNodeFactory")) {
        String label = node.getString("annotation");
        Individual Ivariable = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/variable#" + label);
        if (Ivariable == null) {
          Ivariable = variable.createIndividual("https://w3id.org/semsys/plan/ns/audit/variable#" + label);
        }
        model.createClass("https://w3id.org/semsys/plan/ns/audit#" + label);
        OntClass variableNew = model.getOntClass("https://w3id.org/semsys/plan/ns/audit#" + label);
        entity.addSubClass(variableNew);
        setOutput(node, Ivariable);
        setInput(node, Ivariable);
      } else if (subNode.getString("name").equals("Step") &&
              subNode.getJSONObject("factoryKey").getString("className").equals("org.pplan.step.StepNodeFactory")) {
        String label = node.getString("annotation");
        Individual Istep = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/step#" + label);
        if (Istep == null) {
          Istep = step.createIndividual("https://w3id.org/semsys/plan/ns/audit/step#" + label);
        }
        Istep.addProperty(RDFS.label, label);
        model.createClass("https://w3id.org/semsys/plan/ns/audit#" + label);
        OntClass stepNew = model.getOntClass("https://w3id.org/semsys/plan/ns/audit#" + label);
        activity.addSubClass(stepNew);
        Individual receive = receiving.createIndividual("http://w3id.org/wellfort/prov#" + label);
        receive.addProperty(RDFS.label, label);
        receive.addProperty(type, "JSON");
        setInput(node, Istep);

      }
    }
  }
  private void processVariableNode(JSONObject node) {
    String label = node.getString("annotation");
    Individual Ivariable = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/variable#" + label);
    if (Ivariable == null) {
      Ivariable = variable.createIndividual("https://w3id.org/semsys/plan/ns/audit/variable#" + label);
    }
    Ivariable.addProperty(RDFS.label, label);
    model.createClass("https://w3id.org/semsys/plan/ns/audit#" + label);
    OntClass variableNew = model.getOntClass("https://w3id.org/semsys/plan/ns/audit#" + label);
    entity.addSubClass(variableNew);
    setOutput(node, Ivariable);
    setInput(node, Ivariable);
  }

  private void processStepNode(JSONObject node) {
    String label = node.getString("annotation");
    Individual Istep = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/step#" + label);
    if (Istep == null) {
      Istep = step.createIndividual("https://w3id.org/semsys/plan/ns/audit/step#" + label);
    }
    Istep.addProperty(RDFS.label, label);
    model.createClass("https://w3id.org/semsys/plan/ns/audit#" + label);
    OntClass stepNew = model.getOntClass("https://w3id.org/semsys/plan/ns/audit#" + label);
    activity.addSubClass(stepNew);
    Individual receive = receiving.createIndividual("https://w3id.org/semsys/plan/ns/audit/receiving#" + label);
    receive.addProperty(RDFS.label, label);
    receive.addProperty(type, "JSON");
    setInput(node, Istep);

  }

  private void setOutput(JSONObject node, Individual individual) {
    JSONArray outputs = node.getJSONArray("outputs");
    for (int k = 0; k < outputs.length(); k++) {
      JSONObject output = outputs.getJSONObject(k);
      if (output.has("successors")) {
        JSONArray successors = output.getJSONArray("successors");
        for (int i = 0; i < successors.length(); i++) {
          JSONObject successor = successors.getJSONObject(i);
          JSONArray nodes = jsonObject.getJSONObject("workflow").getJSONArray("nodes");
          for (int j = 0; j < nodes.length(); j++) {
            JSONObject node1 = nodes.getJSONObject(j);
            if (node1.getString("id").equals(successor.getString("id"))) {
              if (node1.getString("name").equals("Variable")
                      && node1.getJSONObject("factoryKey").getString("className").equals("org.pplan.variable.VariableNodeFactory")) {
              } else if (node1.getString("type").equals("Subnode")) {
                JSONObject subWorkflow = node1.getJSONObject("subWorkflow");
                JSONArray subNodes = subWorkflow.getJSONArray("nodes");
                for (int d = 0; d < subNodes.length(); d++) {
                  JSONObject subNode = subNodes.getJSONObject(d);
                  if (subNode.getString("name").equals("Variable") &&
                          subNode.getJSONObject("factoryKey").getString("className").equals("org.pplan.variable.VariableNodeFactory")) {

                  } else if (subNode.getString("name").equals("Step") &&
                          subNode.getJSONObject("factoryKey").getString("className").equals("org.pplan.step.StepNodeFactory")) {
                    String label = node1.getString("annotation");

                    Individual Istep;
                    Individual Ireceive;
                    if (model.getIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label) == null) {
                      Istep = step.createIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label);
                      Ireceive = receiving.createIndividual("http://w3id.org/wellfort/prov#" + label);
                    } else {
                      Istep = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label);
                      Ireceive = model.getIndividual("http://w3id.org/wellfort/prov#" + label);
                    }
                    if (individual.getOntClass().equals(variable)) {
                      Istep.addProperty(this.hasInputVar, individual);
                    } else if (individual.getOntClass().equals(plan)) {
                      individual.addProperty(this.includesPlanElement, Istep);
                      Individual Isystem = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/system#"
                              + node.getJSONArray("settings").getJSONObject(1).getString("value"));
                      Isystem.addProperty(this.hasPlan, individual);
                      Isystem.addProperty(this.hasServiceGenerator, Ireceive);
                    }
                  }
                }
              } else if (node1.getString("name").equals("Step")
                      && node1.getJSONObject("factoryKey").getString("className").equals("org.pplan.step.StepNodeFactory")) {
                String label = node1.getString("annotation");

                Individual Istep;
                Individual Ireceive;
                if (model.getIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label) == null) {
                  Istep = step.createIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label);
                  Ireceive = receiving.createIndividual("http://w3id.org/wellfort/prov#" + label);
                } else {
                  Istep = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label);
                  Ireceive = model.getIndividual("http://w3id.org/wellfort/prov#" + label);
                }
                if (individual.getOntClass().equals(variable)) {
                  Istep.addProperty(this.hasInputVar, individual);
                } else if (individual.getOntClass().equals(plan)) {
                  individual.addProperty(this.includesPlanElement, Istep);
                  Individual Isystem = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/system#"
                          + node.getJSONArray("settings").getJSONObject(1).getString("value"));
                  Isystem.addProperty(this.hasPlan, individual);
                  Isystem.addProperty(this.hasServiceGenerator, Ireceive);
                }
              }
            }
          }
        }
      }
    }
  }

  private void setInput(JSONObject node, Individual individual) {
    JSONArray outputs = node.getJSONArray("outputs");
    for (int k = 0; k < outputs.length(); k++) {
      JSONObject output = outputs.getJSONObject(k);
      if (output.has("successors")) {
        JSONArray successors = output.getJSONArray("successors");
        for (int i = 0; i < successors.length(); i++) {
          JSONObject successor = successors.getJSONObject(i);
          JSONArray nodes = jsonObject.getJSONObject("workflow").getJSONArray("nodes");
          for (int j = 0; j < nodes.length(); j++) {
            JSONObject node1 = nodes.getJSONObject(j);
            if (node1.getString("id").equals(successor.getString("id"))) {
              if (node1.getString("name").equals("Variable")
                      && node1.getJSONObject("factoryKey").getString("className").equals("org.pplan.variable.VariableNodeFactory")) {
                String label = node1.getString("annotation");
                Individual Ivariable = null;
                if (model.getIndividual("https://w3id.org/semsys/plan/ns/audit/variable#" + label) == null) {
                  Ivariable = variable.createIndividual("https://w3id.org/semsys/plan/ns/audit/variable#" + label);
                } else {
                  Ivariable = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/variable#" + label);
                }
                individual.addProperty(this.hasOutputVar, Ivariable);
              } else if (node1.getString("type").equals("Subnode")) {
                JSONObject subWorkflow = node1.getJSONObject("subWorkflow");
                JSONArray subNodes = subWorkflow.getJSONArray("nodes");
                for (int d = 0; d < subNodes.length(); d++) {
                  JSONObject subNode = subNodes.getJSONObject(d);
                  if (subNode.getString("name").equals("Variable") &&
                          subNode.getJSONObject("factoryKey").getString("className").equals("org.pplan.variable.VariableNodeFactory")) {
                    String label = node1.getString("annotation");
                    Individual Ivariable = null;
                    if (model.getIndividual("https://w3id.org/semsys/plan/ns/audit/variable#" + label) == null) {
                      Ivariable = variable.createIndividual("https://w3id.org/semsys/plan/ns/audit/variable#" + label);
                    } else {
                      Ivariable = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/variable#" + label);
                    }
                    individual.addProperty(this.hasOutputVar, Ivariable);

                  } else if (subNode.getString("name").equals("Step") &&
                          subNode.getJSONObject("factoryKey").getString("className").equals("org.pplan.step.StepNodeFactory")) {
                    Individual Istep = null;
                    String label = node1.getString("annotation");
                    if (model.getIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label) == null) {
                      Istep = step.createIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label);
                    } else {
                      Istep = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label);
                    }
                    if (individual.getOntClass().equals(step)) {
                      individual.addProperty(this.isPreceededBy, Istep);
                    }
                  }
                }
              } else if (node1.getString("name").equals("Step")
                      && node1.getJSONObject("factoryKey").getString("className").equals("org.pplan.step.StepNodeFactory")) {
                Individual Istep = null;
                String label = node1.getString("annotation");
                if (model.getIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label) == null) {
                  Istep = step.createIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label);
                } else {
                  Istep = model.getIndividual("https://w3id.org/semsys/plan/ns/audit/step#"+label);
                }
                if (individual.getOntClass().equals(step)) {
                  individual.addProperty(this.isPreceededBy, Istep);
                }
              }
            }
          }
        }
      }
    }
  }



  private JSONObject findNodeById(String nodeId, JSONArray nodes) {
    for (int i = 0; i < nodes.length(); i++) {
      JSONObject node = nodes.getJSONObject(i);
      if (node.getString("id").equals(nodeId)) {
        return node;
      }
    }
    return null;
  }

  private JSONObject findNodeById(String nodeId) {
    JSONArray nodes = jsonObject.getJSONObject("workflow").getJSONArray("nodes");
    return findNodeById(nodeId, nodes);
  }

  public void save() {
    try {
      OutputStream out = new FileOutputStream("src/main/resources/output.ttl");
      model.write(out, "TURTLE");
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }



}
