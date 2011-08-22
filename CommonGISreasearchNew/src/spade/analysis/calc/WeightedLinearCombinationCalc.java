package spade.analysis.calc;

import spade.vis.database.TableStat;

public class WeightedLinearCombinationCalc extends IdealPointCalc {
	@Override
	public void makeInterface() {
		super.makeInterface();
		setTitle("\"Weighted Linear Combination\" decision support method"); //res.getString("Ipdsm"));
	}

	@Override
	public double[] compute() {
		if (tStat == null) {
			tStat = new TableStat();
			tStat.setDataTable(dTable);
		}
		boolean isMax[] = cp.getIsMax();
		float W[] = cp.getWeights();
		double score[] = new double[dTable.getDataItemCount()];
		for (int i = 0; i < score.length; i++) {
			score[i] = 0f;
			for (int k = 0; k < fn.length; k++) {
				double v = dTable.getNumericAttrValue(fn[k], i);
				if (Double.isNaN(v)) {
					score[i] = Double.NaN;
					break;
				}
				if (isMax[k]) {
					v = (v - tStat.getMin(fn[k])) / (tStat.getMax(fn[k]) - tStat.getMin(fn[k]));
				} else {
					v = (tStat.getMax(fn[k]) - v) / (tStat.getMax(fn[k]) - tStat.getMin(fn[k]));
				}
				score[i] += v * W[k];
			}
		}
		return score;
	}
}