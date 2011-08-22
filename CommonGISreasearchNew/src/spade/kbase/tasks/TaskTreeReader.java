package spade.kbase.tasks;

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

public class TaskTreeReader {
	//
	// Parsing an XML document stored in a file.
	//
	protected static LayerRestriction getLayerRestriction(Element restrElement) {
		if (!restrElement.getTagName().equals("Restrictions"))
			return null;
		NodeList restrList = restrElement.getChildNodes();
		if (restrList == null || restrList.getLength() < 1)
			return null;
		LayerRestriction lr = new LayerRestriction();
		for (int j = 0; j < restrList.getLength(); j++)
			if (restrList.item(j) != null && restrList.item(j).getNodeType() == Node.ELEMENT_NODE) {
				Element restr = (Element) restrList.item(j);
				if (restr.getTagName().equals("LayerNumber")) {
					String str = restr.getAttribute("min");
					if (str != null) {
						try {
							int n = Integer.valueOf(str).intValue();
							if (n >= 1) {
								lr.minLayerNumber = n;
							}
						} catch (NumberFormatException nfe) {
						}
					}
					str = restr.getAttribute("max");
					if (str != null)
						if (str.equalsIgnoreCase("any")) {
							lr.maxLayerNumber = Integer.MAX_VALUE;
						} else {
							try {
								int n = Integer.valueOf(str).intValue();
								if (n >= 1) {
									lr.maxLayerNumber = n;
								}
							} catch (NumberFormatException nfe) {
							}
						}
				} else if (restr.getTagName().equals("LayerContent")) {
					lr.allowContentType(restr.getAttribute("contentType"));
				} else if (restr.getTagName().equals("Attributes")) {
					String str = restr.getAttribute("presence");
					lr.mustHaveAttributes = (str != null && str.equalsIgnoreCase("yes"));
					str = restr.getAttribute("minNumber");
					if (str != null) {
						try {
							int n = Integer.valueOf(str).intValue();
							if (n >= 1) {
								lr.minAttrNumber = n;
							}
						} catch (NumberFormatException nfe) {
						}
					}
				}
			}
		return lr;
	}

	protected static AttrRestriction getAttrRestriction(Element restrElement) {
		if (!restrElement.getTagName().equals("Restrictions"))
			return null;
		NodeList restrList = restrElement.getChildNodes();
		if (restrList == null || restrList.getLength() < 1)
			return null;
		AttrRestriction ar = new AttrRestriction();
		for (int j = 0; j < restrList.getLength(); j++)
			if (restrList.item(j) != null && restrList.item(j).getNodeType() == Node.ELEMENT_NODE) {
				Element restr = (Element) restrList.item(j);
				if (restr.getTagName().equals("AttrRestriction")) {
					String str = restr.getAttribute("minNumber");
					if (str != null) {
						try {
							int n = Integer.valueOf(str).intValue();
							if (n >= 1) {
								ar.minNumber = n;
							}
						} catch (NumberFormatException nfe) {
						}
					}
					str = restr.getAttribute("maxNumber");
					if (str != null)
						if (str.equalsIgnoreCase("any")) {
							ar.maxNumber = Integer.MAX_VALUE;
						} else {
							try {
								int n = Integer.valueOf(str).intValue();
								if (n >= 1) {
									ar.maxNumber = n;
								}
							} catch (NumberFormatException nfe) {
							}
						}
					ar.setAllowedType(restr.getAttribute("type"));
					ar.setAllowedRelation(restr.getAttribute("relation"));
				}
			}
		return ar;
	}

	protected static ContextElement getContextElement(Element cEl) {
		if (cEl == null || !cEl.getTagName().equals("ContextElement"))
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
		ce.setType(str);
		//get the remaining attributes
		str = cEl.getAttribute("localId");
		if (str != null && str.length() > 0) {
			ce.localId = str;
		}
		str = cEl.getAttribute("refers_to");
		if (str != null && str.length() > 0) {
			ce.refersTo = str;
		}
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(i);
				if (child.getTagName().equals("Name")) {
					ce.setName(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Explanation")) {
					ce.setExplanation(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Instruction")) {
					ce.setInstruction(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Restrictions"))
					if (ce.getType() == ContextElement.layer) {
						ce.setRestriction(getLayerRestriction(child));
					} else if (ce.getType() == ContextElement.attribute) {
						ce.setRestriction(getAttrRestriction(child));
					}
			}
		return ce;
	}

	protected static Output getOutputSpec(Element el) {
		if (el == null || !el.getTagName().equals("Output"))
			return null;
		Output output = new Output();
		String str = el.getAttribute("localId");
		if (str != null && str.length() > 0) {
			output.localId = str;
		}
		str = el.getAttribute("refers_to");
		if (str != null && str.length() > 0) {
			output.refersTo = str;
		}
		str = el.getAttribute("number");
		if (str != null) {
			output.multiple = str.equalsIgnoreCase("multiple");
		}
		output.setOutputType(el.getAttribute("type"));
		if (output.isValid())
			return output;
		return null;
	}

	protected static VisVariant getVisVariant(Element el) {
		if (el == null || !el.getTagName().equals("Visualization"))
			return null;
		NodeList children = el.getChildNodes();
		if (children == null || children.getLength() < 1)
			return null;
		VisVariant vv = new VisVariant();
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(i);
				if (child.getTagName().equals("Name")) {
					vv.setName(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Explanation")) {
					vv.setExplanation(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Output")) {
					vv.addOutputSpec(getOutputSpec(child));
				}
			}
		String str = el.getAttribute("isDefault");
		vv.isDefault = (str != null && str.equalsIgnoreCase("yes"));
		vv.setVisVariable(el.getAttribute("variable"));
		vv.setVisType(el.getAttribute("type"));
		vv.setVisMethod(el.getAttribute("method"));
		str = el.getAttribute("complexity");
		if (str != null) {
			try {
				int k = Integer.valueOf(str).intValue();
				if (k > 0) {
					vv.complexity = k;
				}
			} catch (NumberFormatException nfe) {
			}
		}
		if (vv.isValid())
			return vv;
		return null;
	}

	protected static VisReq getVisRequirements(Element el) {
		if (el == null || !el.getTagName().equals("VisDesign"))
			return null;
		NodeList children = el.getElementsByTagName("Visualization");
		if (children == null || children.getLength() < 1)
			return null;
		String str = el.getAttribute("applyTo");
		if (str == null || str.length() < 1)
			return null;
		VisReq vr = new VisReq();
		vr.setApplyTo(str);
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				vr.addVisVariant(getVisVariant((Element) children.item(i)));
			}
		if (vr.isValid())
			return vr;
		return null;
	}

	public static VisCombination getVisCombination(Element el) {
		if (el == null || !el.getTagName().equals("VisCombination"))
			return null;
		NodeList children = el.getElementsByTagName("VisDesign");
		if (children == null || children.getLength() < 1)
			return null;
		VisCombination vc = new VisCombination();
		vc.setCombinationMethod(el.getAttribute("method"));
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				vc.addVisComponent(getVisRequirements((Element) children.item(i)));
			}
		if (vc.getNComponents() > 0)
			return vc;
		return null;
	}

	protected static Input getInputSpec(Element el) {
		if (el == null || !el.getTagName().equals("Input"))
			return null;
		String str = el.getAttribute("arguments");
		if (str == null || str.length() < 1)
			return null;
		Input input = new Input();
		input.setArguments(str);
		str = el.getAttribute("optional");
		input.isOptional = (str != null && str.equalsIgnoreCase("yes"));
		return input;
	}

	protected static Input getAltInputSpec(Element el) {
		if (el == null || !el.getTagName().equals("AltInput"))
			return null;
		NodeList children = el.getElementsByTagName("Input");
		if (children == null || children.getLength() < 1)
			return null;
		AltInput ainp = new AltInput();
		ainp.setArguments(el.getAttribute("arguments"));
		String str = el.getAttribute("optional");
		ainp.isOptional = (str != null && str.equalsIgnoreCase("yes"));
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				ainp.addAlternative(getInputSpec((Element) children.item(i)));
			}
		return ainp;
	}

	protected static OperationSpec getOperationSpecification(Element el) {
		if (el == null || !el.getTagName().equals("Operation"))
			return null;
		NodeList children = el.getChildNodes();
		if (children == null || children.getLength() < 1)
			return null;
		String str = el.getAttribute("type");
		if (str == null || str.length() < 1)
			return null;
		OperationSpec op = new OperationSpec();
		op.type = str;
		str = el.getAttribute("isDefault");
		op.isDefault = (str != null && str.equalsIgnoreCase("yes"));
		str = el.getAttribute("results_mapped");
		op.resultsMapped = (str != null && str.equalsIgnoreCase("yes"));
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(i);
				if (child.getTagName().equals("Name")) {
					op.setName(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Instruction")) {
					op.setInstruction(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("Output")) {
					op.addOutput(getOutputSpec(child));
				} else if (child.getTagName().equals("Input")) {
					op.addInput(getInputSpec(child));
				} else if (child.getTagName().equals("AltInput")) {
					op.addInput(getAltInputSpec(child));
				}
			}
		return op;
	}

	protected static ContextMapping getContextMapping(Element el) {
		if (el == null || !el.getTagName().equals("ContextMapping"))
			return null;
		ContextMapping cmap = new ContextMapping();
		String str = el.getAttribute("Task");
		if (str != null && str.length() > 0) {
			cmap.taskId = str;
		}
		str = el.getAttribute("From");
		if (str != null && str.length() > 0) {
			cmap.from = str;
		}
		str = el.getAttribute("To");
		if (str != null && str.length() > 0) {
			cmap.to = str;
		}
		if (cmap.isValid())
			return cmap;
		return null;
	}

	protected static AndContextMapping getAndContextMapping(Element el) {
		if (el == null || !el.getTagName().equals("AndContextMapping"))
			return null;
		String str = el.getAttribute("Task");
		if (str == null || str.length() < 1)
			return null;
		NodeList children = el.getElementsByTagName("ContextMapping");
		if (children == null || children.getLength() < 1)
			return null;
		AndContextMapping cmap = new AndContextMapping();
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				cmap.addContextMapping(getContextMapping((Element) children.item(i)));
			}
		if (cmap.isValid())
			return cmap;
		return null;
	}

	protected static NextTaskSpec getNextTask(Element el) {
		if (el == null || !el.getTagName().equals("NextTask"))
			return null;
		NodeList children = el.getChildNodes();
		if (children == null || children.getLength() < 1)
			return null;
		NextTaskSpec next = new NextTaskSpec();
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element) children.item(i);
				if (child.getTagName().equals("Explanation")) {
					next.setExplanation(MultiLangSupport.getCurrentLanguageText(child));
				} else if (child.getTagName().equals("ContextMapping")) {
					next.addContextMapping(getContextMapping(child));
				} else if (child.getTagName().equals("AndContextMapping")) {
					next.addContextMapping(getAndContextMapping(child));
				}
			}
		return next;
	}

	public static TaskTree readTaskTree(String fname) {
		Document doc = null;
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
		if (doc == null)
			return null;
		NodeList nl = doc.getDocumentElement().getElementsByTagName("TreeNode");
		if (nl == null || nl.getLength() < 1)
			return null;
		TaskTree tree = new TaskTree();
		for (int i = 0; i < nl.getLength(); i++)
			if (nl.item(i) != null && nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element taskElem = (Element) nl.item(i);
				if (!taskElem.hasChildNodes()) {
					continue;
				}
				String id = taskElem.getAttribute("Identifier");
				if (id == null) {
					continue;
				}
				TreeNode tn = new TreeNode(id, taskElem.getAttribute("Parent"));
				NodeList children = taskElem.getChildNodes();
				for (int j = 0; j < children.getLength(); j++)
					if (children.item(j) != null && children.item(j).getNodeType() == Node.ELEMENT_NODE) {
						Element child = (Element) children.item(j);
						if (child.getTagName().equals("Name")) {
							tn.setName(MultiLangSupport.getCurrentLanguageText(child));
						} else if (child.getTagName().equals("Explanation")) {
							tn.setExplanation(MultiLangSupport.getCurrentLanguageText(child));
						} else if (child.getTagName().equals("NextTask")) {
							tn.addNextTaskSpec(getNextTask(child));
						} else if (child.getTagName().equals("Context")) {
							NodeList contexts = child.getElementsByTagName("ContextElement");
							if (contexts != null && contexts.getLength() > 0) {
								if (tn.context == null) {
									tn.context = new Vector(contexts.getLength(), 3);
								}
								for (int k = 0; k < contexts.getLength(); k++)
									if (contexts.item(k) != null) {
										ContextElement ce = getContextElement((Element) contexts.item(k));
										if (ce != null) {
											tn.context.addElement(ce);
										}
									}
							}
						} else if (child.getTagName().equals("MapView")) {
							tn.showDataOnMap = child.getAttribute("show_data");
							NodeList instrList = child.getElementsByTagName("Instruction");
							if (instrList != null && instrList.getLength() > 0 && instrList.item(0) != null && instrList.item(0).getNodeType() == Node.ELEMENT_NODE) {
								tn.mapViewInstruction = MultiLangSupport.getCurrentLanguageText(instrList.item(0));
							}
						} else if (child.getTagName().equals("VisDesign")) {
							tn.setVisRequirements(getVisRequirements(child));
						} else if (child.getTagName().equals("VisCombination")) {
							tn.setVisCombination(getVisCombination(child));
						} else if (child.getTagName().equals("Operation")) {
							tn.addOperationSpec(getOperationSpecification(child));
						}
					}
				tree.addTask(tn);
			}
		if (tree != null) {
			tree.propagateConstraints();
		}
		return tree;
	}

}
