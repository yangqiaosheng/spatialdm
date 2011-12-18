There are two different types of models:

1) Model of normal traffic by times of the day and day of the week
2) Model simulating additional traffic, e.g., a large number of cars 
moving from an area around a stadium after a football game.

There are two java classes to run these two types of models:

1) NormalTrafficPredictor - runs the model of the normal traffic.

Predicts the presence (number) of cars in cells of a territory division
(e.g. Voronoi polygons) by time intervals. The length of the time
intervals is determined by the model, which is loaded from an XML file.
An example of such a model is in the file 
models/model_place_presence_15min_intervals.xml
As the name suggests, the model predicts values for 15-minute time intervals.

2) TrafficSimulator - runs the model simulating additional (abnormal) traffic.

The model simulates the movement of a given number of cars from pre-specified
areas (the areas are chosen at the stage of model building and cannot be
changed afterwards). The simulation is done by 1-minute time steps starting 
from a given time moment (specified as a parameter) and ending when the last 
car arrives at the destination place; hence, the length of the simulation
time period is not known in advance. The total time may differ depending on
the starting time, since the normal traffic (i.e., the loads of the traffic
links) is taken into account in computing the speeds of the cars.
After the simulation is done, the results are aggregated by time intervals
of the length specified by the user.
There are two examples of such simulation models:
models/sim_model.xml and models/sim_model_reduced_north.xml
Both models simulate movement from the area of the stadium San Siro in Milan.
The difference is that in the first model more cars move to the north of the
stadium and along the northern motorway, which gets very much overloaded 
resulting in big delays. In the second model, a part of the traffic is 
re-routed to other roads, which decreases the total time of the movement.

Both classes NormalTrafficPredictor and TrafficSimulator are included in 
one JAR file TrafficSIM.jar. There are two example batch files to run each of them:
runNormalTrafficPredictor.bat  and  runTrafficSim.bat

Both classes read model parameters from a text file. The path to this file
must be specified as the program argument. There are three example paths
with the parameters:
params1.txt and params2.txt - for TrafficSimulator
params3.txt - for NormalTrafficPredictor

NormalTrafficPredictor requires the following parameters:
 a) path to the model, an XML file with the document tag NumTSModelSet
 b) start date and time in the format dd/mm/yyyy;hh:tt (tt denotes minutes)
 c) end date and time in the same format
 d) whether to add Gaussian noise (true or false)
 e) path to the output file
The parameters b,c, and d should be specified by the user. The model should be
chosen by the user from the set of available models based on the annotations. 
 
TrafficSimulator requires the following parameters:
 a) path to the model, an XML file with the document tag MovementSimulationModel 
 b) start date and time in the format dd/mm/yyyy;hh:tt (tt denotes minutes)
 c) optionally, the number of simulated cars, if it is different from the 
    number in the model.
    For example, the model is built for 10000 cars but the user wants simulation
    for 12000 or 7500 cars. In this case, the model will be scaled (but it is
    not guaranteed that this will result in exactly 12000 or 7500 cars due to
	rounding errors).
 d) the length of the intervals for data aggregation, in minutes
 e) path to the output file
The parameters b,c, and d should be specified by the user. The model should be
chosen by the user from the set of available models based on the annotations. 

Both NormalTrafficPredictor and TrafficSimulator produce the output file
in the same CSV format:
1st line: names of the columns. 1st column: identifiers of the areas (polygons). 
All other columns contain loads of the areas (i.e. numbers of cars in them) by 
time intervals. The beginnings of the intervals are specified in the column names.
All following lines: identifier of an area followed by the counts of cars.

This CSV file does not contain the coordinates of the area boundaries. 
The coordinates are specified in an XML file data/places.xml

The subdirectory java contains the source codes of the classes that are used,
except for the library OpenForecast, which is an opensource library downloaded
from the Web.

The classes for reading XML files with coordinates (like places.xml) are not
included since they are not used for running the models. A code for reading such 
files can be found among the CommonGIS codes, see spade.vis.dmap.GeoToXML 
and data_load.readers.XMLReader. These codes cannot be reused directly
since they use specific CommonGIS data structures but they can be adapted
for the use in ESS.

It is not necessary to create first a CSV file with model results and then transform 
it to KML. The codes of the classes TrafficSimulator and NormalTrafficPredictor 
can be modified to write the output directly in a KML file.