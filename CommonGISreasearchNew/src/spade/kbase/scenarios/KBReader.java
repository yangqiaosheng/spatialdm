package spade.kbase.scenarios;

import java.io.File;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
* Reads the task knowledge base (specified in XML format) into internal
* structures. Uses the libraries for XML parsing provided by Sun Microsystems.
*/
public class KBReader {
	protected Document doc = null;

	//
	// Parsing an XML document stored in a file.
	//
	protected Restriction getRestriction(Element el) {
		if (el == null || !el.getTagName().equals("Restriction"))
			return null;
		Restriction restr = new Restriction();
		String str = el.getAttribute("type");
		if (str != null && str.length() > 0) {
			restr.setRestrictionType(str);
		}
		str = el.getAttribute("value");
		if (str != null && str.length() > 0) {
			restr.setValues(str);
		}
		if (restr.isValid())
			return restr;
		return null;
	}

	protected ContextElement getContextElement(Element cEl) {
		if (cEl == null || !cEl.getTagName().equals("Context"))
			return null;
		//get the type; if null, ignore
		String str = cEl.getAttribute("type");
		if (str == null || str.length() < 1)
			return null;
		//get children elements of the context element
		NodeList children = cEl.getChildNodes();
		if (children == null || children.getLength() < 1)
			return null;
		ContextElement ce = new ContextElement();
		ce.setContextType(str);
		//get the remaining attributes
		str = cEl.getAttribute("id");
		if (str != null && str.length() > 0) {
			ce.localId = str;
		}
		str = cEl.getAttribute("refers_to");
		if (str != null && str.length() > 0) {
			ce.refersTo = str;
		}
		str = cEl.getAttribute("is_optional");
		if (str != null && str.length() > 0) {
			ce.isOptional = str.equals("yes");
		}
		str = cEl.getAttribute("method");
		if (str != null && str.length() > 0) {
			ce.setMethod(str);
		}
		str = cEl.getAttribute("help_topic");
		if (str != null && str.length() > 0) {
			ce.setHelpTopicId(str);
		}
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(i);
				if (child.getTagName().equals("Name")) {
					ce.setName(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Prompt")) {
					ce.setPrompt(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Instruction")) {
					ce.setInstruction(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Restriction")) {
					ce.addRestriction(getRestriction(child));
				}
			}
		return ce;
	}

	protected Priority getPriority(Element el) {
		if (el == null || !el.getTagName().equals("Priority"))
			return null;
		Priority prior = new Priority();
		String str = el.getAttribute("refers_to");
		if (str != null && str.length() > 0) {
			prior.refersTo = str;
		}
		str = el.getAttribute("order");
		if (str != null && str.length() > 0) {
			prior.setOrder(str);
		}
		return prior;
	}

	protected Output getOutputSpec(Element el) {
		if (el == null || !el.getTagName().equals("Output"))
			return null;
		Output output = new Output();
		String str = el.getAttribute("id");
		if (str != null && str.length() > 0) {
			output.localId = str;
		}
		str = el.getAttribute("refers_to");
		if (str != null && str.length() > 0) {
			output.refersTo = str;
		}
		output.setOutputType(el.getAttribute("type"));
		if (output.isValid())
			return output;
		return null;
	}

	protected Input getInputSpec(Element el) {
		if (el == null || !el.getTagName().equals("Input"))
			return null;
		String str = el.getAttribute("argument");
		if (str == null || str.length() < 1)
			return null;
		Input input = new Input();
		input.arg = str;
		str = el.getAttribute("stands_for");
		if (str != null && str.length() > 0) {
			input.standsFor = str;
		}
		return input;
	}

	protected Instrument getInstrumentSpecification(Element el) {
		if (el == null || !el.getTagName().equals("Instrument"))
			return null;
		String str = el.getAttribute("type");
		if (str == null || str.length() < 1)
			return null;
		Instrument instr = new Instrument();
		instr.setInstrumentType(str);
		str = el.getAttribute("function");
		if (str != null && str.length() > 0) {
			instr.function = str;
		}
		str = el.getAttribute("is_default");
		instr.isDefault = (str != null && str.equalsIgnoreCase("yes"));
		str = el.getAttribute("useRequired");
		if (str != null && str.length() > 0) {
			instr.setRequiredUses(str);
		}
		str = el.getAttribute("useDesired");
		if (str != null && str.length() > 0) {
			instr.setDesiredUses(str);
		}
		str = el.getAttribute("special");
		if (str != null && str.length() > 0) {
			instr.setSpecialDemands(str);
		}
		NodeList children = el.getChildNodes();
		if (children == null)
			return instr;
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(i);
				if (child.getTagName().equals("Name")) {
					instr.setName(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Explanation")) {
					instr.setExplanation(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Instruction")) {
					instr.setInstruction(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Input")) {
					instr.addInput(getInputSpec(child));
				} else if (child.getTagName().equals("Restriction")) {
					instr.addRestriction(getRestriction(child));
				} else if (child.getTagName().equals("Priority")) {
					instr.addPriority(getPriority(child));
				}
			}
		//System.out.println(instr.toString());
		return instr;
	}

	protected ToolInput getToolInputSpec(Element el) {
		if (el == null || !el.getTagName().equals("ToolInput"))
			return null;
		String id = el.getAttribute("arg_id");
		if (id == null || id.length() < 1)
			return null;
		String type = el.getAttribute("type");
		if (type == null || type.length() < 1)
			return null;
		ToolInput input = new ToolInput();
		input.arg_id = id;
		input.arg_type = type;
		String str = el.getAttribute("refers_to");
		if (str != null && str.length() > 0) {
			input.refersTo = str;
		}
		input.setMin(el.getAttribute("min"));
		input.setMax(el.getAttribute("max"));
		NodeList children = el.getChildNodes();
		if (children == null)
			return input;
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(i);
				if (child.getTagName().equals("Restriction")) {
					Restriction r = getRestriction(child);
					if (r != null) {
						//System.out.println(id+": "+r.toString());
						input.addRestriction(r);
					}
				}
			}
		return input;
	}

	protected Tool getToolDescription(Element el) {
		if (el == null || !el.getTagName().equals("Tool"))
			return null;
		NodeList children = el.getChildNodes();
		if (children == null || children.getLength() < 1)
			return null;
		String str = el.getAttribute("function");
		if (str == null || str.length() < 1)
			return null;
		Tool instr = new Tool();
		instr.setFunction(str);
		str = el.getAttribute("variant");
		if (str != null && str.length() > 0) {
			instr.variant = str;
		}
		instr.setComplexity(el.getAttribute("complexity"));
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(i);
				if (child.getTagName().equals("Name")) {
					instr.setName(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Explanation")) {
					instr.setExplanation(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Instruction")) {
					str = child.getAttribute("use");
					if (str != null && str.length() > 0) {
						instr.addInstruction(MultiLangSupport.getCurrentLanguageText(child), str);
					} else {
						instr.addInstruction(MultiLangSupport.getCurrentLanguageText(child));
					}
				} else if (child.getTagName().equals("Output")) {
					instr.addOutput(getOutputSpec(child));
				} else if (child.getTagName().equals("ToolInput")) {
					instr.addInput(getToolInputSpec(child));
				}
			}
		//System.out.println(instr.toString());
		return instr;
	}

	protected Manipulator getManipulator(Element el) {
		if (el == null || !el.getTagName().equals("Manipulator"))
			return null;
		NodeList children = el.getChildNodes();
		if (children == null || children.getLength() < 1)
			return null;
		String str = el.getAttribute("use");
		if (str == null || str.length() < 1)
			return null;
		Manipulator man = new Manipulator();
		man.setSupportedTasks(str);
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(i);
				if (child.getTagName().equals("Name")) {
					man.setName(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Explanation")) {
					man.setExplanation(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Instruction")) {
					str = child.getAttribute("use");
					if (str != null && str.length() > 0) {
						man.addInstruction(MultiLangSupport.getCurrentLanguageText(child), str);
					} else {
						man.addInstruction(MultiLangSupport.getCurrentLanguageText(child));
					}
				}
			}
		return man;
	}

	protected boolean loadKB(String fname) {
		doc = null;
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.parse(new File(fname));
			// normalize text representation
			doc.getDocumentElement().normalize();
		} catch (SAXParseException err) {
			System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
			System.out.println("   " + err.getMessage());
			// print stack trace as below
		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return doc != null;
	}

	public TaskKBase getKB(String fname) {
		if (!loadKB(fname))
			return null;
		TaskKBase kb = new TaskKBase();
		kb.tasks = getTaskTree();
		if (kb.tasks == null)
			return null;
		kb.visMethods = getVisualizations();
		kb.primTasks = getPrimitiveTasks();
		kb.tools = getToolDescriptions();
		return kb;
	}

	protected Vector getPrimitiveTasks() {
		if (doc == null)
			return null;
		NodeList nl = doc.getDocumentElement().getElementsByTagName("PrimitiveTask");
		if (nl == null || nl.getLength() < 1)
			return null;
		Vector ptasks = new Vector(nl.getLength(), 5);
		for (int i = 0; i < nl.getLength(); i++)
			if (nl.item(i) != null && nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) nl.item(i);
				if (!elem.hasChildNodes()) {
					continue;
				}
				String id = elem.getAttribute("id");
				if (id == null) {
					continue;
				}
				NodeList names = elem.getElementsByTagName("Name");
				if (names == null || names.getLength() < 1) {
					continue;
				}
				String name = MultiLangSupport.getCurrentLanguageText(names.item(0));
				PrimitiveTask pt = new PrimitiveTask(id, name);
				ptasks.addElement(pt);
			}
		if (ptasks.size() < 1)
			return null;
		return ptasks;
	}

	protected Vector getToolDescriptions() {
		if (doc == null)
			return null;
		NodeList nl = doc.getDocumentElement().getElementsByTagName("Tool");
		if (nl == null || nl.getLength() < 1)
			return null;
		Vector tools = new Vector(nl.getLength(), 5);
		for (int i = 0; i < nl.getLength(); i++)
			if (nl.item(i) != null && nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) nl.item(i);
				if (!elem.hasChildNodes()) {
					continue;
				}
				Tool t = getToolDescription(elem);
				if (t != null) {
					tools.addElement(t);
				}
			}
		if (tools.size() < 1)
			return null;
		return tools;
	}

	protected TaskTree getTaskTree() {
		if (doc == null)
			return null;
		NodeList nl = doc.getDocumentElement().getElementsByTagName("Node");
		if (nl == null || nl.getLength() < 1)
			return null;
		TaskTree tree = new TaskTree();
		for (int i = 0; i < nl.getLength(); i++)
			if (nl.item(i) != null && nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element taskElem = (Element) nl.item(i);
				if (!taskElem.hasChildNodes()) {
					continue;
				}
				String id = taskElem.getAttribute("id");
				if (id == null) {
					continue;
				}
				TreeNode tn = new TreeNode(id, taskElem.getAttribute("parent"));
				tn.setNodeType(taskElem.getAttribute("type"));
				NodeList children = taskElem.getChildNodes();
				for (int j = 0; j < children.getLength(); j++)
					if (children.item(j) != null && children.item(j).getNodeType() == Node.ELEMENT_NODE) {
						Element child = (Element) children.item(j);
						if (child.getTagName().equals("Name")) {
							String str = child.getAttribute("type");
							if (str != null && str.equals("short")) {
								tn.setShortName(MultiLangSupport.getCurrentLanguageText(child));
							} else {
								tn.setName(MultiLangSupport.getCurrentLanguageText(child));
							}
						} else if (child.getTagName().equals("Explanation")) {
							tn.setExplanation(MultiLangSupport.getCurrentLanguageText(child));
						} else if (child.getTagName().equals("Context")) {
							ContextElement cel = getContextElement(child);
							if (cel != null) {
								cel.setTaskId(tn.getId());
								tn.addContext(cel);
							}
						} else if (child.getTagName().equals("Instrument")) {
							tn.addInstrument(getInstrumentSpecification(child));
						} else if (child.getTagName().equals("Restriction")) {
							tn.addRestriction(getRestriction(child));
						}
					}
				tree.addTask(tn);
			}
		return tree;
	}

	protected VisCollection getVisualizations() {
		if (doc == null)
			return null;
		NodeList nl = doc.getDocumentElement().getElementsByTagName("Visualization");
		if (nl == null || nl.getLength() < 1)
			return null;
		VisCollection visCol = new VisCollection();
		for (int i = 0; i < nl.getLength(); i++)
			if (nl.item(i) != null && nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element taskElem = (Element) nl.item(i);
				if (!taskElem.hasChildNodes()) {
					continue;
				}
				Visualization vis = new Visualization();
				vis.setVariable(taskElem.getAttribute("variable"));
				vis.setMethod(taskElem.getAttribute("method"));
				vis.setComplexity(taskElem.getAttribute("complexity"));
				vis.setSupportedTasks(taskElem.getAttribute("use"));
				NodeList children = taskElem.getChildNodes();
				for (int j = 0; j < children.getLength(); j++)
					if (children.item(j) != null && children.item(j).getNodeType() == Node.ELEMENT_NODE) {
						Element child = (Element) children.item(j);
						if (child.getTagName().equals("Name")) {
							vis.setName(MultiLangSupport.getCurrentLanguageText(child));
						} else if (child.getTagName().equals("Restriction")) {
							vis.addRestriction(getRestriction(child));
						} else if (child.getTagName().equals("Explanation")) {
							vis.setExplanation(MultiLangSupport.getCurrentLanguageText(child));
						} else if (child.getTagName().equals("Instruction")) {
							String str = child.getAttribute("use");
							if (str != null && str.length() > 0) {
								vis.addInstruction(MultiLangSupport.getCurrentLanguageText(child), str);
							} else {
								vis.addInstruction(MultiLangSupport.getCurrentLanguageText(child));
							}
						} else if (child.getTagName().equals("Manipulator")) {
							vis.addManipulator(getManipulator(child));
						}
					}
				visCol.addVisMethod(vis);
			}
		return visCol;
	}

}
