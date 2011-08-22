public class RestrictedGermanTexts {
	public RestrictedGermanTexts() {
		System.out.println("German texts loaded");
		new core.Res_de();
		new data_load.Res_de();
		new data_load.readers.Res_de();
		new spade.analysis.classification.Res_de();
		new spade.analysis.manipulation.Res_de();
		new spade.analysis.plot.Res_de();
		new spade.lib.basicwin.Res_de();
		new spade.lib.color.Res_de();
		new spade.lib.font.Res_de();
		new spade.vis.database.Res_de();
		new spade.vis.dataview.Res_de();
		new spade.vis.dmap.Res_de();
		new spade.vis.event.Res_de();
		new spade.vis.mapvis.Res_de();
		new spade.vis.preference.Res_de();
		new spade.vis.space.Res_de();
		new ui.Res_de();
		new ui.bitmap.Res_de();
	}
}