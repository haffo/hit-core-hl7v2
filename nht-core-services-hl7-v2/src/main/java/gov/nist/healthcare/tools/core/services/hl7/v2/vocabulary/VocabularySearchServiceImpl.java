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
package gov.nist.healthcare.tools.core.services.hl7.v2.vocabulary;
 
import gov.nist.healthcare.tools.core.models.TableDefinition;
import gov.nist.healthcare.tools.core.models.TableElement;
import gov.nist.healthcare.tools.core.models.VocabularyCollection;
import gov.nist.healthcare.tools.core.models.VocabularyLibrary;
import gov.nist.healthcare.tools.core.models.VocabularySearchResult;
import gov.nist.healthcare.tools.core.models.VocabularySearchResultItem;
import gov.nist.healthcare.tools.core.repo.VocabularyCollectionRepository;
import gov.nist.healthcare.tools.core.services.TableLibraryUnmarshaller;
import gov.nist.healthcare.tools.core.services.VocabConstants;
import gov.nist.healthcare.tools.core.services.VocabularySearchService;
import gov.nist.healthcare.tools.core.services.exception.VocabularySearchException;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author Harold Affo (NIST)
 */
 public class VocabularySearchServiceImpl extends VocabularySearchService {

 
  	private TableLibraryUnmarshaller umarshaller;
  
 	private VocabularyCollectionRepository vocabularyCollectionRepository;

	public VocabularySearchServiceImpl() {
	} 
	
	public VocabularySearchServiceImpl(TableLibraryUnmarshaller umarshaller,
			VocabularyCollectionRepository vocabularyCollectionRepository) {
		super();
		this.umarshaller = umarshaller;
		this.vocabularyCollectionRepository = vocabularyCollectionRepository;
	}






	public VocabularyCollectionRepository getVocabularyCollectionRepository() {
		return vocabularyCollectionRepository;
	}
 
	public void setVocabularyCollectionRepository(
			VocabularyCollectionRepository vocabularyCollectionRepository) {
		this.vocabularyCollectionRepository = vocabularyCollectionRepository;
	}

  

//	@PostConstruct
//	public void init() {
//		cachedVocabularyCollections = vocabularyCollectionRepository.findAll();
//		if (!cachedVocabularyCollections.isEmpty()) {
//			Iterator<VocabularyCollection> it = cachedVocabularyCollections
//					.iterator();
//			while (it.hasNext()) {
//				VocabularyCollection vocab = it.next();
//				Iterator<VocabularyLibrary> it2 = vocab.getVocabularyLibraries()
//						.iterator();
//				while (it2.hasNext()) {
//					VocabularyLibrary vocabLibrary = it2.next();
//					String content = vocabLibrary.getContent();
//					VocabularyLibrary tLibrary = umarshaller.unmarshall(IOUtils
//							.toInputStream(content));
//					vocabLibrary.load(tLibrary.getTableDefinition(),
//							tLibrary.getDescription(),
//							tLibrary.getHL7Version(), tLibrary.getName(),
//							tLibrary.getOrganizationName(),
//							tLibrary.getStatus(),
//							tLibrary.getTableLibraryIdentifier(),
//							tLibrary.getTableLibraryVersion());
//				}
//			}
//		}
//	}

	private static Logger logger = Logger
			.getLogger(VocabularySearchServiceImpl.class);

	/**
	 * This searchTableElementByCodeAndDescription method takes searchValue and
	 * SearchCriteria as input and search all the table from the xml and returns
	 * collection of string array having matching value and related table info.
	 * 
	 * @param searchValue
	 *            the search value
	 * @param searchCriteria
	 *            the search criteria
	 * @return the collection
	 * @throws VocabularySearchException
	 *             the illegal argument exception
	 */

 	@Override
	public Collection<VocabularySearchResultItem> searchByTestContextId(Long testContextId,String searchValue,
			String searchCriteria) throws VocabularySearchException {
		if (StringUtils.isEmpty(searchValue)
				|| StringUtils.isBlank(searchValue)) {
			String message = "please enter a valid input.";
			logger.error(message);
			throw new VocabularySearchException(message);
		}
		if (testContextId == null) {
			String message = "Missing testContext";
			logger.error(message);
			throw new VocabularySearchException(message);
		}
		
		if (StringUtils.isEmpty(searchCriteria)
				|| StringUtils.isBlank(searchCriteria)) {
			String message = "please select valid search criteria.";
			logger.error(message);
			throw new VocabularySearchException(message);
		}
		String searchQuery = escapeRegex(searchValue.trim());
		searchQuery = ".*(" + searchQuery + ").*";

		String searchFor = searchQuery;
		Pattern pattern = Pattern.compile(searchFor, Pattern.CASE_INSENSITIVE);

		List<VocabularySearchResultItem> searchResults = new ArrayList<VocabularySearchResultItem>();
		List<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();

		int sortBy = 0;

		try {
			List<VocabularyCollection> vocabCollectionList = getVocabularyCollectionByTestCaseContextId(testContextId);

			if (VocabConstants.VALUE_SEARCH.equalsIgnoreCase(searchCriteria)) {
				// For value, we have to search for the exact code
				pattern = Pattern.compile(searchValue.trim());
				tempList = performTableValueSearch(vocabCollectionList,pattern);
				// this is the column where Value will be displayed on the UI
				sortBy = 3;
			}

			if (VocabConstants.DESC_SEARCH.equalsIgnoreCase(searchCriteria)) {
 				tempList = performTableDescriptionSearch(vocabCollectionList,pattern);
				// this is the column where Descripition will be displayed on
				// the UI
				sortBy = 4;
			}

			if (VocabConstants.VALUE_SET_CODE_SEARCH
					.equalsIgnoreCase(searchCriteria)) {
 				tempList = performValueSetCodeSearch(vocabCollectionList,pattern);
				// this is the column where Source will be displayed on the UI
				sortBy = 0;
			}

			if (VocabConstants.VALUE_SET_NAME_SEARCH
					.equalsIgnoreCase(searchCriteria)) {
 				tempList = performValueSetNameSearch(vocabCollectionList,pattern);
				// this is the column where Table Name will be displayed on the
				// UI
				sortBy = 2;
			}

			if (VocabConstants.TABLE_ID_SEARCH.equalsIgnoreCase(searchCriteria)) {
 				tempList = performTableIdSearch(vocabCollectionList,pattern);
				// this is the column where Table Id will be displayed on the UI
				sortBy = 1;
			}
			// sort the search results by the user selected search criteria
			searchResults = sortSearchResults(tempList, sortBy);

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("HL7tableLibrary or table may be null");
			logger.error(ex, ex);
			// throw new VocabSearchException(
			// "HL7tableLibrary or table may be null");
		}
		return searchResults;
	}

	/**
	 * 
	 * @param input
	 * @return
	 */
	private static String escapeRegex(String input) {
		final StringBuilder result = new StringBuilder();

		final StringCharacterIterator iterator = new StringCharacterIterator(
				input);
		char character = iterator.current();
		while (character != CharacterIterator.DONE) {
			/*
			 * All literals need to have backslashes doubled.
			 */
			if (character == '.') {
				result.append("\\.");
			} else if (character == '\\') {
				result.append("\\\\");
			} else if (character == '?') {
				result.append("\\?");
			} else if (character == '*') {
				result.append("\\*");
			} else if (character == '+') {
				result.append("\\+");
			} else if (character == '&') {
				result.append("\\&");
			} else if (character == ':') {
				result.append("\\:");
			} else if (character == '{') {
				result.append("\\{");
			} else if (character == '}') {
				result.append("\\}");
			} else if (character == '[') {
				result.append("\\[");
			} else if (character == ']') {
				result.append("\\]");
			} else if (character == '(') {
				result.append("\\(");
			} else if (character == ')') {
				result.append("\\)");
			} else if (character == '^') {
				result.append("\\^");
			} else if (character == '$') {
				result.append("\\$");
			} else {
				// the char is not a special one
				// add it to the result as is
				result.append(character);
			}
			character = iterator.next();
		}
		return result.toString();
	}

	private List<VocabularySearchResultItem> sortSearchResults(
			List<VocabularySearchResultItem> tempList, final int sortBy) {
		if (tempList != null && !(tempList.isEmpty())) {
			Collections.sort(tempList,
					new Comparator<VocabularySearchResultItem>() {
						@Override
						public int compare(VocabularySearchResultItem one,
								VocabularySearchResultItem another) {
							return String.valueOf(one.getCodeSys()).compareTo(
									String.valueOf(another.getCodeSys()));
						}
					});
		}
		return tempList;
	}

	private List<VocabularySearchResultItem> performTableValueSearch(List<VocabularyCollection> vocabCollectionList,
			Pattern pattern) {
 		List<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();
		for (VocabularyCollection collection : vocabCollectionList) {
			for (VocabularyLibrary lib : collection.getVocabularyLibraries()) {
				tempList.addAll(VocabularySearchUtils.performValueSearch(
						pattern, lib));
			}
		}
		return tempList;
	}

	private List<VocabularySearchResultItem> performTableDescriptionSearch(List<VocabularyCollection> vocabCollectionList,
			Pattern pattern) {
 		List<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();
		for (VocabularyCollection collection : vocabCollectionList) {
			for (VocabularyLibrary lib : collection.getVocabularyLibraries()) {
				tempList.addAll(VocabularySearchUtils.performDescriptionSearch(
						pattern, lib));

			}
		}
		return tempList;
	}

	private List<VocabularySearchResultItem> performValueSetCodeSearch(List<VocabularyCollection> vocabCollectionList,
			Pattern pattern) {
		List<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();
 		for (VocabularyCollection collection : vocabCollectionList) {
			for (VocabularyLibrary lib : collection.getVocabularyLibraries()) {
				tempList.addAll(VocabularySearchUtils
						.performValueSetCodeSearch(pattern, lib));

			}
		}
		return tempList;
	}

	private List<VocabularySearchResultItem> performValueSetNameSearch(List<VocabularyCollection> vocabCollectionList,
			Pattern pattern) {
 		List<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();
		for (VocabularyCollection collection : vocabCollectionList) {
			for (VocabularyLibrary lib : collection.getVocabularyLibraries()) {
				tempList.addAll(VocabularySearchUtils
						.performValueSetNameSearch(pattern, lib));

			}
		}
		return tempList;
	}

	private List<VocabularySearchResultItem> performTableIdSearch(List<VocabularyCollection> vocabCollectionList,
			Pattern pattern) {
 		List<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();
		for (VocabularyCollection collection : vocabCollectionList) {
			for (VocabularyLibrary lib : collection.getVocabularyLibraries()) {
				tempList.addAll(VocabularySearchUtils.performTableIdSearch(
						pattern, lib));

			}
		}
		return tempList;
	}

	// @Override
	// public VocabularySearchResult searchTableValues(
	// VocabularySearchResult searchContent) {
	// searchContent.setTableContent(null);
	// searchContent.setValueSetName(null);
	// searchContent.setTableNumber(null);
	// searchContent.setValueSetCode(null);
	// searchContent.setComment(null);
	// searchContent.setTableType(null);
	// searchContent.setValueSetOID(null);
	// if (searchContent.getSelectCriteria().startsWith("Table")
	// || searchContent.getSelectCriteria().startsWith("ValueSet")) {
	// searchContent.setSearchResultsColumnCount(3);
	// } else {
	// searchContent.setSearchResultsColumnCount(5);
	// }
	// searchContent
	// .setSearchTablesDetailContent(searchTableElementByCodeAndDescription(
	// searchContent.getSearchString(),
	// searchContent.getSelectCriteria()));
	// if (searchContent.getSearchTablesDetailContent().isEmpty()) {
	// String message = "No results found for entered search criteria.";
	// logger.error(message);
	// throw new VocabSearchException(message);
	// }
	// return searchContent;
	// }

	public TableLibraryUnmarshaller getUmarshaller() {
		return umarshaller;
	}

	public void setUmarshaller(TableLibraryUnmarshaller umarshaller) {
		this.umarshaller = umarshaller;
	}

	public static class VocabularySearchUtils {

		/**
		 * Initialize the tables and the tableList instances from the
		 * tablelibrary object
		 * 
		 * @param tableLibrary
		 */
		public static void loadTableList(VocabularyLibrary vocabularyLibrary) {

			HashMap<Integer, TableDefinition> tables = new HashMap<Integer, TableDefinition>();
			int id = 0;
			for (Iterator<TableDefinition> iterator = vocabularyLibrary
					.getTableDefinition().iterator(); iterator.hasNext();) {
				TableDefinition tableDefinition = iterator.next();
				if (checkTableCondition(tableDefinition)) {
					tables.put(id, tableDefinition);
				}
				id++;
			}
			List<TableDefinition> tableList = new ArrayList<TableDefinition>(
					tables.values());
			Collections.sort(tableList, new Comparator<TableDefinition>() {

				@Override
				public int compare(TableDefinition one, TableDefinition other) {
					return compareTable(one, other);
				}
			});
			vocabularyLibrary.setTableList(tableList);
		}

		/**
		 * condition that a table must verify before being added to the table
		 * list
		 * 
		 * @param table
		 *            : table to check the condition on
		 * @return: result of the checking
		 */
		public static boolean checkTableCondition(TableDefinition table) {
			return true;
		}

		/**
		 * @param pattern
		 * @return
		 */

		public static Collection<VocabularySearchResultItem> performValueSearch(
				Pattern pattern, VocabularyLibrary vocabularyLibrary) {
			Collection<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();
			for (TableDefinition table : vocabularyLibrary.getTableList()) {
				for (TableElement tableElement : table.getTableElement()) {
					if (pattern.matcher(tableElement.getCode().trim())
							.matches()) {
						tempList.add(new VocabularySearchResultItem(
								tableElement.getCodesys() == null ? table
										.getCodesys() : tableElement
										.getCodesys(), getTypeOrId(table),
								table.getName(), tableElement.getCode(),
								tableElement.getDisplayName()));
					}
				}
			}
			return tempList;
		}

		/**
		 * @param tableList
		 * @param pattern
		 * @param hl7
		 * @return
		 */

		public static Collection<VocabularySearchResultItem> performDescriptionSearch(
				Pattern pattern, VocabularyLibrary vocabularyLibrary) {
			Collection<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();
			for (TableDefinition table : vocabularyLibrary.getTableList()) {
				for (TableElement tableElement : table.getTableElement()) {
					if (pattern.matcher(tableElement.getDisplayName().trim())
							.matches()) {
						String typeOrId = getTypeOrId(table);
						String codeSys = tableElement.getCodesys();
						if (codeSys == null)
							codeSys = table.getCodesys();
						tempList.add(new VocabularySearchResultItem(codeSys,
								typeOrId, table.getName(), tableElement
										.getCode(), tableElement
										.getDisplayName()));
					}
				}
			}
			return tempList;
		}

		/**
		 * @param tableList
		 * @param pattern
		 * @return
		 */
		public static Collection<VocabularySearchResultItem> performValueSetCodeSearch(
				Pattern pattern, VocabularyLibrary vocabularyLibrary) {
			Collection<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();
			for (TableDefinition table : vocabularyLibrary.getTableList()) {
				String codeSys = table.getCodesys();
				if (pattern.matcher(codeSys.trim()).matches()) {
					String typeOrId = getTypeOrId(table);
					tempList.add(new VocabularySearchResultItem(codeSys,
							typeOrId, table.getName()));
				}
			}
			return tempList;
		}

		/**
		 * @param tableList
		 * @param pattern
		 * @return
		 */
		public static Collection<VocabularySearchResultItem> performValueSetNameSearch(
				Pattern pattern, VocabularyLibrary vocabularyLibrary) {
			Collection<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();
			for (TableDefinition table : vocabularyLibrary.getTableList()) {
				if (pattern.matcher(table.getName().trim()).matches()) {
					String typeOrId = getTypeOrId(table);
					String codeSys = table.getCodesys();
					tempList.add(new VocabularySearchResultItem(codeSys,
							typeOrId, table.getName()));
				}
			}
			return tempList;
		}

		public static void validateInput(String value) {
			if (StringUtils.isEmpty(value) || StringUtils.isBlank(value)) {
				String message = "Invalid arguments passed";
				throw new VocabularySearchException(message);
			}
		}

		public static Collection<VocabularySearchResultItem> filterByTableName(
				String tableName) {
			return new ArrayList<VocabularySearchResultItem>();
		}

		/**
		 * Compare two table definitions.
		 * 
		 * @param one
		 * @param other
		 * @return
		 */

		public static int compareTable(TableDefinition one,
				TableDefinition other) {
			if (one.getIdNumber() != null && other.getIdNumber() != null)
				return one.getIdNumber().compareTo(other.getIdNumber());
			else if (one.getId() != null && other.getId() != null)
				return one.getId().compareTo(other.getId());
			return 0;
		}

		public static String getTypeOrId(TableDefinition table) {
			return table.getId();
		}

		/**
		 * This searchByTableNumber method takes tableNumber as input search
		 * through the TableLibrary List and return the table elements related
		 * to that particular HL7 table number.
		 * 
		 * @param tableNumber
		 *            the table Number
		 * @return the collection of TableElement
		 * @throws VocabularySearchException
		 *             the illegal argument exception
		 */
		public static TableDefinition getTableDefinitionByTableId(
				String tableId, VocabularyLibrary vocabularyLibrary)
				throws VocabularySearchException {
			TableDefinition tableDefintion = null;

			if (StringUtils.isEmpty(tableId) || StringUtils.isBlank(tableId)) {
				String message = "Invalid arguments passed";
				logger.error(message);
				throw new VocabularySearchException(message);
			}
			for (TableDefinition table : vocabularyLibrary.getTableList()) {
				if (tableId.trim().equalsIgnoreCase(table.getId().trim())) {
					tableDefintion = table;
					break;
				}
			}
			return tableDefintion;
		}

		/**
		 * This searchByTableNumber method takes tableNumber as input search
		 * through the TableLibrary List and return the table elements related
		 * to that particular HL7 table number.
		 * 
		 * @param tableNumber
		 *            the table Number
		 * @return the collection of TableElement
		 * @throws VocabularySearchException
		 *             the illegal argument exception
		 */
		private static Collection<VocabularySearchResultItem> searchByTableNumber(
				String tableNumber, VocabularySearchResult result,
				VocabularyLibrary vocabularyLibrary) {
			logger.info("Searching for table number: " + tableNumber);
			if (StringUtils.isEmpty(tableNumber)
					|| StringUtils.isBlank(tableNumber)) {
				String message = "Invalid arguments passed";
				logger.error(message);
				throw new VocabularySearchException(message);
			}

			ArrayList<VocabularySearchResultItem> searchResults = null;
			Collection<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();
			result.setSearchResultsColumnCount(2);
			try {
				for (TableDefinition table : vocabularyLibrary.getTableList()) {
					if (table.getName() != null
							&& table.getId().toLowerCase().trim()
									.equalsIgnoreCase(tableNumber.trim())) {
						for (TableElement hl7TableElement : table
								.getTableElement()) {
							String codeSys = hl7TableElement.getCodesys();
							tempList.add(new VocabularySearchResultItem(
									hl7TableElement.getCode(), hl7TableElement
											.getDisplayName(), codeSys, table
											.getType(), table.getOid(), table
											.getCodesys(), table.getComment()));
							if (codeSys != null
									&& result.getSearchResultsColumnCount() == 2) {
								result.setSearchResultsColumnCount(3);
							}

						}

					}
				}
			} catch (NullPointerException ex) {
				logger.error("Table Library or table may be null");
				throw new VocabularySearchException(
						"Table Library or table may be null");
			}
			HashSet<VocabularySearchResultItem> hashSet = new HashSet<VocabularySearchResultItem>(
					tempList);
			searchResults = new ArrayList<VocabularySearchResultItem>(hashSet);
			if (searchResults.size() != 0) {
				Collections.sort(searchResults,
						new Comparator<VocabularySearchResultItem>() {
							@Override
							public int compare(VocabularySearchResultItem one,
									VocabularySearchResultItem another) {
								return String.valueOf(one.getCode()).compareTo(
										String.valueOf(another.getCode()));
							}
						});
			}
			return searchResults;
		}

		public static VocabularySearchResult searchTable(String tableNumber,
				String valueSetName, VocabularyLibrary vocabularyLibrary) {
			VocabularySearchResult result = new VocabularySearchResult();
			result.setValueSetName(valueSetName);
			result.setTableNumber(tableNumber);
			result.setSearchTablesDetailContent(null);
			result.setComment(null);
			if (tableNumber != null && !(tableNumber.trim().isEmpty())) {
				result.setTableContent(searchByTableNumber(tableNumber, result,
						vocabularyLibrary));
				for (Iterator<VocabularySearchResultItem> tableContentIterator = result
						.getTableContent().iterator(); tableContentIterator
						.hasNext();) {
					VocabularySearchResultItem tableElement = tableContentIterator
							.next();
					result.setTableType(tableElement.getTypeOrId());
					result.setValueSetOID(tableElement.getOid());
					result.setValueSetCode(tableElement.getTableCodeSys());
					result.setComment(tableElement.getComment());
					break;
				}
				// For some tables, there are no TableElements which contain
				// value,
				// description fields
				if (result.getTableContent() == null
						|| result.getTableContent().isEmpty()) {
					TableDefinition definition = getTableDefinitionByTableId(
							tableNumber, vocabularyLibrary);
					if (definition != null) {
						result.setValueSetCode(definition.getCodesys());
						result.setTableType(definition.getType());
						result.setValueSetOID(definition.getOid());
						result.setComment(definition.getComment());
					}
				}
			}
			return result;

		}

		public static VocabularySearchResult searchTableNumber(
				String tableNumber, VocabularyLibrary vocabularyLibrary) {
			VocabularySearchResult result = new VocabularySearchResult();
			result.setTableNumber(tableNumber);
			result.setSearchTablesDetailContent(null);
			result.setSearchTablesDetailContent(null);
			result.setTableContent(searchByTableNumber(tableNumber, result,
					vocabularyLibrary));
			if (result.getTableContent() == null) {
				String message = "Table Information not found";
				throw new VocabularySearchException(message);
			}
			for (Iterator<VocabularySearchResultItem> tableContentIterator = result
					.getTableContent().iterator(); tableContentIterator
					.hasNext();) {
				VocabularySearchResultItem tableElement = tableContentIterator
						.next();
				result.setTableType(tableElement.getTypeOrId());
				result.setValueSetOID(tableElement.getOid());
				result.setValueSetCode(tableElement.getTableCodeSys());
				result.setComment(tableElement.getComment());

				break;
			}
			return result;

		}

		public static VocabularySearchResult searchTable(String tableNumber,
				VocabularyLibrary vocabularyLibrary) {
			VocabularySearchResult result = new VocabularySearchResult();
			result.setTableNumber(tableNumber);
			result.setSearchTablesDetailContent(null);
			result.setTableContent(searchByTableNumber(result.getTableNumber(),
					result, vocabularyLibrary));
			if (result.getTableContent() == null) {
				String message = "Table Information not found";
				throw new VocabularySearchException(message);
			}
			for (Iterator<VocabularySearchResultItem> tableContentIterator = result
					.getTableContent().iterator(); tableContentIterator
					.hasNext();) {
				VocabularySearchResultItem tableElement = tableContentIterator
						.next();
				result.setTableType(tableElement.getTypeOrId());
				result.setValueSetOID(tableElement.getOid());
				result.setValueSetCode(tableElement.getTableCodeSys());
				result.setComment(tableElement.getComment());
				break;
			}
			return result;
		}

		public static boolean contain(TableDefinition table, String tableName) {
			return table.getName() != null
					&& table.getId().toLowerCase().trim()
							.equalsIgnoreCase(tableName.trim());
		}

		public static Collection<VocabularySearchResultItem> performTableIdSearch(
				Pattern pattern, VocabularyLibrary vocabularyLibrary) {
			Collection<VocabularySearchResultItem> tempList = new ArrayList<VocabularySearchResultItem>();
			// Only HL7 table library definitions have Id in them
			for (TableDefinition table : vocabularyLibrary.getTableList()) {
				if (pattern.matcher(table.getId().trim()).matches()) {
					String codeSys = table.getCodesys();
					tempList.add(new VocabularySearchResultItem(codeSys, table
							.getId(), table.getName(), table.getComment()));
				}
			}
			return tempList;
		}

	}

	@Override
	public VocabularySearchResult searchTable(Long vocabCollectionId,
			Long vocabularyLibraryId, String tableNumber, String valueSetName)
			throws VocabularySearchException {
		VocabularyLibrary library = findVocabularyLibrary(vocabCollectionId,
				vocabularyLibraryId);
		if (library == null) {
			throw new VocabularySearchException(
					"We cannot find the vocabulary specified. Please refresh and try again");
		}
		return VocabularySearchUtils.searchTable(tableNumber, valueSetName,
				library);

	}

	private VocabularyLibrary findVocabularyLibrary(Long collectionId,
			Long libraryId) {
		if (collectionId != null && libraryId != null) {
			VocabularyCollection collection = getVocabularyCollection(collectionId);
			for (VocabularyLibrary library : collection.getVocabularyLibraries()) {
				if (libraryId.equals(library.getId())) {
					return library;
				}
			}
		}
		return null;
	}

	@Override
	public VocabularyCollection getVocabularyCollection(Long id) {
		VocabularyCollection collection = vocabularyCollectionRepository
				.findOne(id);
		if (collection != null) {
			Iterator<VocabularyLibrary> it2 = collection.getVocabularyLibraries()
					.iterator();
			while (it2.hasNext()) {
				VocabularyLibrary vocabLibrary = it2.next();
				String content = vocabLibrary.getContent();
				VocabularyLibrary tLibrary = umarshaller.unmarshall(IOUtils
						.toInputStream(content));
				if(tLibrary != null){
				vocabLibrary.load(tLibrary.getTableDefinition(),
						tLibrary.getDescription(), tLibrary.getHL7Version(),
						tLibrary.getName(), tLibrary.getOrganizationName(),
						tLibrary.getStatus(),
						tLibrary.getTableLibraryIdentifier(),
						tLibrary.getTableLibraryVersion());
				}
			}
		}
		return collection;
	}

 

	@Override
	public List<VocabularyCollection> getVocabularyCollectionByTestCaseContextId(
			Long testCaseContextId) {
		List<VocabularyCollection> tmp =  vocabularyCollectionRepository.findAllByTestContextId(testCaseContextId);
 		if(tmp != null){
 		Iterator<VocabularyCollection> it = tmp
				.iterator();
		while (it.hasNext()) {
			VocabularyCollection vocab = it.next();
			Iterator<VocabularyLibrary> it2 = vocab.getVocabularyLibraries()
					.iterator();
			while (it2.hasNext()) {
				VocabularyLibrary vocabLibrary = it2.next();
				String content = vocabLibrary.getContent();
				VocabularyLibrary tLibrary = umarshaller.unmarshall(IOUtils
						.toInputStream(content));
				vocabLibrary.load(tLibrary.getTableDefinition(),
						tLibrary.getDescription(),
						tLibrary.getHL7Version(), tLibrary.getName(),
						tLibrary.getOrganizationName(),
						tLibrary.getStatus(),
						tLibrary.getTableLibraryIdentifier(),
						tLibrary.getTableLibraryVersion());
			}
		}
 		}
 		return tmp;
	}

}
