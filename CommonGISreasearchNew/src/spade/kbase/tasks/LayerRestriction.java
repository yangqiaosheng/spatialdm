package spade.kbase.tasks;

public class LayerRestriction {
	public static final String contentTypes[] = { "entities", "occurrences", "territory_division", "sample_locations", "grid" };

	public int minLayerNumber = 1, maxLayerNumber = Integer.MAX_VALUE;
	public boolean contentAllowed[] = null;
	public boolean mustHaveAttributes = false;
	public int minAttrNumber = 1;

	public void allowContentType(String contentType) {
		if (contentType == null)
			return;
		boolean any = contentType.equalsIgnoreCase("any");
		if (contentAllowed == null) {
			contentAllowed = new boolean[contentTypes.length];
			for (int i = 0; i < contentTypes.length; i++) {
				contentAllowed[i] = any;
			}
			if (any)
				return;
		}
		for (int i = 0; i < contentTypes.length; i++)
			if (any || contentType.equalsIgnoreCase(contentTypes[i])) {
				contentAllowed[i] = true;
				if (!any) {
					break;
				}
			}
	}

	public boolean isContentTypeAllowed(String contentType) {
		if (contentType == null)
			return false;
		if (contentAllowed == null)
			return true;
		if (contentType.equalsIgnoreCase("any")) {
			for (int i = 0; i < contentTypes.length; i++)
				if (!contentAllowed[i])
					return false;
			return true;
		}
		for (int i = 0; i < contentTypes.length; i++)
			if (contentType.equalsIgnoreCase(contentTypes[i]))
				return contentAllowed[i];
		return false; //unknown content type
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("LayerRestriction:\nallowed contents:");
		if (contentAllowed == null) {
			sb.append(" any");
		} else {
			for (int i = 0; i < contentTypes.length; i++)
				if (contentAllowed[i]) {
					sb.append(" " + contentTypes[i]);
				}
		}
		sb.append("\nminLayerNumber=" + minLayerNumber + " maxLayerNumber=" + maxLayerNumber + "\nmustHaveAttributes=" + mustHaveAttributes);
		if (mustHaveAttributes) {
			sb.append(" minAttrNumber=" + minAttrNumber);
		}
		return sb.toString();
	}
}