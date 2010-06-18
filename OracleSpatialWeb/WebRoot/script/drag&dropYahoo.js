// Now we need to create our YUI instance and tell it to load the dd-drag  module.
//YUI().use('dd-drag'
//Now that we have a YUI instance with the dd-drag module, we need to instantiate the Drag instance on this Node
YUI().use('dd-drag', function(Y) {
   // var dd1 = new Y.DD.Drag({         
  //      node: '#move'
 //   });    
});

YUI().use('dd-plugin', function(Y) {
	var node = Y.one('#container');
	node.plug(Y.Plugin.Drag);

	//Now you can only drag it from the x in the corner
		node.dd.addHandle('#carousel');
	});

YUI().use('dd-plugin', function(Y) {
    var node = Y.one('#move');
    node.plug(Y.Plugin.Drag);
 
    //Now you can only drag it from the x in the corner
    node.dd.addHandle('#idmove'); 
});
