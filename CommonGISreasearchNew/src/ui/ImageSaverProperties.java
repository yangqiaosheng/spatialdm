package ui;

public interface ImageSaverProperties {

	public String getSelectedFormat();

	public boolean isSaveMap();

	public boolean isSaveLegend();

	public boolean isSaveMapAsIs();

	public boolean isSaveMapAndLegend();

	public float getJPEGQuality();

	public int getPNGCompression();

	public String fmt2MimeType(String fmtExt);
}
