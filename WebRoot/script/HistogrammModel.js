/* A model for the histogram is dataArray which is passed as a first argument when creating HistogrammModel object*/
function HistogrammModel(dataArray, vb1, vb2, vb3, vb4, vb5) {
	this.data = dataArray;
	this.valueB1 = vb1;
	this.valueB2 = vb2;
	this.valueB3 = vb3;
	this.valueB4 = vb4;
	this.valueB5 = vb5;
};
HistogrammModel.prototype.getIndexValueOfBool = function(){
	if (this.valueB1 == true){return 1;}
	if (this.valueB2 == true){return 2;}
	if (this.valueB3 == true){return 3;}
	if (this.valueB4 == true){return 4;}
	if (this.valueB5 == true){return 5;}
};

HistogrammModel.prototype.toString = function(index) {
	return '' + this.data;
};

HistogrammModel.prototype.getNumberOfClasses = function() {
	return this.data.length;
};

HistogrammModel.prototype.getValueAt = function(index) {
	return this.data[index];
};


HistogrammModel.prototype.setValueAt = function(index, entry) {
	this.data[index] = entry;
};

