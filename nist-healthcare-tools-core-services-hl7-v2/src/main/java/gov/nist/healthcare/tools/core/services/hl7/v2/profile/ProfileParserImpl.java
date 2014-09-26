/**
 * This software was developed at the National Institute of Standards and Technology by employees
 * of the Federal Government in the course of their official duties. Pursuant to title 17 Section 105 of the
 * United States Code this software is not subject to copyright protection and is in the public domain.
 * This is an experimental system. NIST assumes no responsibility whatsoever for its use by other parties,
 * and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic.
 * We would appreciate acknowledgement if the software is used. This software can be redistributed and/or
 * modified freely provided that any derivative works bear some notice that they are derived from it, and any
 * modified versions bear some notice that they have been modified.
 */
package gov.nist.healthcare.tools.core.services.hl7.v2.profile;

import gov.nist.healthcare.core.hl7.v2.enumeration.Usage;
import gov.nist.healthcare.core.hl7.v2.model.ComponentModel;
import gov.nist.healthcare.core.hl7.v2.model.DataElementModel;
import gov.nist.healthcare.core.hl7.v2.model.ElementModel;
import gov.nist.healthcare.core.hl7.v2.model.FieldModel;
import gov.nist.healthcare.core.hl7.v2.model.GroupModel;
import gov.nist.healthcare.core.hl7.v2.model.Predicate;
import gov.nist.healthcare.core.hl7.v2.model.SegmentModel;
import gov.nist.healthcare.core.hl7.v2.model.SubComponentModel;
import gov.nist.healthcare.core.hl7.v2.util.MessageModelBuilder; 
import gov.nist.healthcare.tools.core.models.profile.ProfileElement;
import gov.nist.healthcare.tools.core.models.profile.ProfileModel;
import gov.nist.healthcare.tools.core.services.profile.ProfileParser;
import gov.nist.healthcare.tools.core.services.profile.ProfileParserException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProfileParserImpl implements ProfileParser {

	private final static String GROUP = "GROUP";
	private final static String SEGMENT = "SEGMENT";
	private final static String FIELD = "FIELD";
	private final static String COMPONENT = "COMPONENT";
	private final static String SUBCOMPONENT = "SUBCOMPONENT";
	
 	public ProfileParserImpl() {		
 	}
 	 
	@Override	
	public ProfileModel parse(String content, Object...options) throws ProfileParserException {
		ProfileModel model = new ProfileModel();
		try {
			Set<String> tracker = new HashSet<String>();
 			MessageModelBuilder mmb = new MessageModelBuilder();
			gov.nist.healthcare.core.hl7.v2.model.ProfileModel profileModel = mmb
					.buildModel(content);
			gov.nist.healthcare.core.hl7.v2.model.MessageModel messageModel = profileModel
					.getMessageModel();

			List<gov.nist.healthcare.core.hl7.v2.model.ElementModel> srcElementModels = messageModel
					.getChildrenModel();
 			ProfileElement fullElementModel = new ProfileElement("FULL");
 			model.getElements().add(fullElementModel);
			for (gov.nist.healthcare.core.hl7.v2.model.ElementModel elModel : srcElementModels) {
				process(elModel, fullElementModel,model,tracker);
			}

		} catch (Exception e) { 
			e.printStackTrace();
			throw new ProfileParserException(e.getMessage());
		}
 		return model;
	}

	private void process(
			gov.nist.healthcare.core.hl7.v2.model.ElementModel elementModel,
			ProfileElement parentElement, ProfileModel model,Set<String> tracker ) {
		if (elementModel == null)
			return;
		if (elementModel instanceof GroupModel) {
			ProfileElement groupElement =  getProfileElement(
					(GroupModel) elementModel);
			//if(parentElement.getChildren() !=null){
				parentElement.getChildren().add(groupElement);
			//}
			for (gov.nist.healthcare.core.hl7.v2.model.ElementModel e : elementModel
					.getChildrenModel()) {
				process(e, groupElement,model,tracker);
			}
		} else if (elementModel instanceof SegmentModel) {
			ProfileElement segmentElement =  getProfileElement(
					(SegmentModel) elementModel);
			//if(parentElement.getChildren() != null){
				parentElement.getChildren().add(segmentElement);
			//}
			if (!tracker.contains(elementModel.getName())) {
				model.getElements().add(segmentElement);
				tracker.add(elementModel.getName());
			}
			
		}
	}

	
	
	public ProfileElement getProfileElement(SegmentModel model) {
		ProfileElement segmentElement = new ProfileElement();
		buildAttributes(model, segmentElement);
		segmentElement.setLongName(model.getLongName());
		segmentElement.setTitle( segmentElement.getName() + " : " + segmentElement.getLongName());
 		segmentElement.setChildren(buildChildren(model));
 		return segmentElement;
	}

	public ProfileElement getProfileElement(GroupModel model) {
		ProfileElement groupElement = new ProfileElement();
		buildAttributes(model,groupElement);
		groupElement.setLongName("");
		groupElement.setTitle(groupElement.getName());
		return groupElement;
	}

	/**
	 * @param model
	 * @return
	 */
	protected String getPath(DataElementModel model) {
		if (model instanceof FieldModel) {
			FieldModel fieldmodel = (FieldModel) model;
			return ((ElementModel) fieldmodel.getParent()).getName() + "."
					+ model.getPosition();
		} else if (model instanceof ComponentModel) {
			return getPath((DataElementModel) ((ComponentModel) model)
					.getParent()) + "." + model.getPosition();
		} else if (model instanceof SubComponentModel) {
			return getPath((DataElementModel) ((SubComponentModel) model)
					.getParent()) + "." + model.getPosition();
		}
		return "";
	}

	/**
	 * @return
	 */
	public String getUsage(ElementModel model) {
		Usage usage = model.getUsage();
		String code = usage.getCode();
		if ("C".equals(code) || "CE".equals(code)) {
			if (model.getPredicate() != null) {
				Usage trueUsage = model.getPredicateTrueUsage();
				Usage falseUsage = model.getPredicateFalseUsage();
				if (trueUsage != null && falseUsage != null) {
					return "C" + "(" + trueUsage.getCode() + "/"
							+ falseUsage.getCode() + ")";
				}
			}
		}

		return code;
	}

	public String getIcon(ElementModel model) {
		if (model instanceof GroupModel) {
			return "group.png";
		} else if (model instanceof SegmentModel) {
			return "segment.png";
		} else if (model instanceof FieldModel) {
			return "field.png";
		} else if (model instanceof ComponentModel) {
			return "component.png";
		} else if (model instanceof SubComponentModel) {
			return "subcomponent.png";
		}
		return "";
	}

	public String getType(ElementModel model) {
		if (model instanceof GroupModel) {
			return GROUP;
		} else if (model instanceof SegmentModel) {
			return SEGMENT;
		} else if (model instanceof FieldModel) {
			return FIELD;
		} else if (model instanceof ComponentModel) {
			return COMPONENT;
		} else if (model instanceof SubComponentModel) {
			return SUBCOMPONENT;
		}
		throw new RuntimeException("Unknow profile element type "
				+ model.getClass());
	}

	 
	private void buildAttributes(ElementModel model, ProfileElement element) {
		element.setUsage(getUsage(model));
		element.setType(getType(model));
		Predicate predicateObj = model.getPredicate();
		if (predicateObj != null) {
			element.setPredicate(predicateObj.getEnglishDescription());
		}
		element.setImplementationNote(model.getImplementationNote());
		element.setReference(model.getReference());
		element.setConformanceStatement(model.getConformanceStatementList());
		element.setMinOccurs(model.getMinOccurs());
		element.setMaxOccurs(model.getMaxOccurs());
		element.setCardinality("[" + model.getMinOccurs() + "," + model.getMaxOccurs() + "]");
 		element.setDataTypeUsage("O".equals(element.getUsage()) && model instanceof FieldModel ? "-": element.getUsage());
 		element.setIcon(getIcon(model));
 		element.setName( model.getName());
 		if (model instanceof DataElementModel) {
			DataElementModel data = (DataElementModel) model;
			element.setMinLength(data.getMinLength() + "");
			element.setMaxLength(data.getMaxLength() + "");
			element.setTable(data.getTable());
			element.setDataType(data.getDataType());
			element.setPredicateFalseUsage(data.getPredicateFalseUsage().getCode());
			element.setPredicateTrueUsage(data.getPredicateTrueUsage().getCode());
			element.setPosition(data.getPosition() + "");
		}
		String minLength = element.getMinLength();
		String maxLength = element.getMaxLength();
		if (minLength != null && !minLength.equals("-1") && maxLength != null
				&& !maxLength.equals("")) {
			if (!maxLength.equals("65K")) {
				element.setLength("[" + minLength + "," + maxLength + "]");
			} else {
				element.setLength("[" + minLength + ",*]");
			}
		} else {
			element.setLength("");
		}
	}
	
	
	
	public ProfileElement getSegmentChildElement(ElementModel model) {
		ProfileElement childElement = new ProfileElement();
		if (model instanceof SegmentModel) {
			SegmentModel segment = ((SegmentModel) model);			
			childElement.setName(segment.getName());
			childElement.setLongName(segment.getLongName());
			childElement.setTitle( segment.getName() + " : " + segment.getLongName());
			childElement.setType(getType(segment));
			childElement.setUsage(getUsage(model));
			childElement.setChildren(buildChildren(segment));
		} else { 			
			buildAttributes(model,   childElement); 
		}
		return childElement;
	}


	/**
	 * 
	 * build the data tree
	 */
	public List<ProfileElement> buildChildren(
			gov.nist.healthcare.core.hl7.v2.model.SegmentModel segmentModel) {
		List<ProfileElement> children = new ArrayList<ProfileElement>();
		if (segmentModel.getChildrenModel() != null) {
			for (gov.nist.healthcare.core.hl7.v2.model.ElementModel fieldModel : segmentModel
					.getChildrenModel()) {
				ProfileElement fieldElement = getSegmentChildElement(
						fieldModel);
				children.add(fieldElement);
				// crate field note
				if (fieldModel.getChildrenModel() != null) {
					List<ProfileElement> fChildren = new ArrayList<ProfileElement>();
					fieldElement.setChildren(fChildren);
					for (gov.nist.healthcare.core.hl7.v2.model.ElementModel componentModel : fieldModel
							.getChildrenModel()) {
						ProfileElement componentElement = getSegmentChildElement(
								componentModel);
						fChildren.add(componentElement);
						if (componentModel.getChildrenModel() != null) {
							List<ProfileElement> compChildren = new ArrayList<ProfileElement>();
							componentElement.setChildren(compChildren);
							for (gov.nist.healthcare.core.hl7.v2.model.ElementModel subComponentModel : componentModel
									.getChildrenModel()) {
								compChildren.add(getSegmentChildElement(
										subComponentModel));
							}
						}
					}
				}
			}
		}
		return children;
	}
}
 