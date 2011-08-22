package spade.analysis.tools.schedule;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 19-Feb-2008
 * Time: 15:58:58
 */
public class SchedulerTexts_tools_schedule extends java.util.ListResourceBundle {
	static final Object[][] contents = new String[][] { { "sch_start", "Start the scheduler" }, { "sch_stop", "Stop the scheduler" }, { "sch_load", "Load a schedule" }, { "sch_summary", "Summary view" }, { "sch_gantt", "Line (Gantt) chart" },
			{ "sch_matrix", "Source-destination matrix" }, { "sch_print", "Write orders in text file" }, { "schedule", "Schedule" }, { "no_map_exists", "No map exists!" }, { "no_map_layers", "No map layers available!" },
			{ "confirm_unload", "The previous schedule and all related data will be unloaded. Do you wish to proceed?" }, { "remove_prev_schedule", "Remove previous schedule?" }, { "try_load_distances_from_", "Trying to load distances from " },
			{ "wait_aggr_progress", "Wait... Data aggregation is in progress" }, { "got_data_vehicle_use", "Successfully got data for the vehicle use display!" },
			{ "failed_get_data_vehicle_use", "Failed to get data for the vehicle use display..." }, { "schedule_summary", "Schedule summary" }, { "pos_vehicles", "Positions of vehicles" }, { "Gantt_chart_", "Gantt chart: " },
			{ "move_matrix_", "Movement matrix: " }, { "all_cat", "All categories" }, { "item_cat", "Item category" }, { "time_int_", "Time interval:" }, { "from", "from" }, { "till", "till" },
			{ "Load_latest_schedule", "Load the latest version of the schedule" }, { "Load_schedule", "Load schedule" }, { "Load_schedule_version", "Do you wish to load a version of the schedule" },
			{ "latest_schedule", "the latest version of the schedule" }, { "final_schedule_prev_run", "the final version of the last completed run" }, { "allowed_time_", "Allowed running time (minutes):" }, { "invalid_time", "Invalid time!" },
			{ "Error!", "Error!" }, { "failed_init_scheduler", "Failed to initialize the external scheduler!" }, { "failed_make_setup_file", "Failed to open or create the setup file for the external scheduler!" },
			{ "cannot_write_to", "Cannot write to" }, { "scheduler_stopped", "The scheduler has been stopped" }, { "all", "all" }, { "scheduler_finished", "The scheduler has finished its work!" },
			{ "scheduler_run_complete", "The scheduler has completed a run." }, { "wait_time_passed", "The allowed waiting time has passed." }, { "no_result_yet", "No result has been produced yet..." },
			{ "let_scheduler_continue_", "Let the scheduler continue?" }, { "scheduler_status", "Scheduler status" }, { "first_version_produced", "A first version of the schedule has been produced" },
			{ "new_version_produced", "A new version of the schedule has been produced" }, { "load_final_schedule_prev_run", "Load the final version of the last completed run" }, { "try_load_distances_from", "Trying to load distances from" },
			{ "loaded_distances_from", "Loaded distances from" }, { "failed_load_vehicle_types", "Failed to load information about the types of vehicles!" }, { "Got", "Got" }, { "vehicle_classes", "vehicle classes" },
			{ "vehicle_class_names", "vehicle class names" }, { "failed_get_vehicle_class_names", "Failed to get vehicle class names!" }, { "loaded_veh_cap_table_with", "Loaded the vehicle capacity table with" }, { "records", "records" },
			{ "Item_sources", "Item sources" }, { "Destinations", "Destinations" }, { "Vehicle_locations", "Vehicle locations" }, { "Places", "Places" }, { "Load_dist_from_file_", "Load pre-computed distances between the sites from a file?" },
			{ "Use_pre_comp_dist_", "Use pre-computed distances?" }, { "Spec_file_distances", "Specify the file with the distances" }, { "failed_load_distances_from", "Failed to load distances from" }, { "Try_another_file_", "Try another file?" },
			{ "av_speed_request", "Average vehicle speed (km/h)?" }, { "av_speed_expl", "To compute the driving times, the average vehicle speed must be specified." }, { "Average_speed", "Average speed" },
			{ "Could_not_create_file", "Could not create file" }, { "Error_writing_file", "Error in writing data to file" }, { "Destinations_UI_title", "Destinations for the items to evacuate" }, { "Number_of_items", "Number of items" },
			{ "Available_capacity", "Available capacity" }, { "Items_to_evacuate_", "Items to evacuate:" }, { "possible_destinations_and_capacities", "Possible destinations and their capacities" }, { "curr_site_", "Current site:" },
			{ "add_item_cat", "Add an item category" }, { "remove_site", "Remove the site" }, { "add_site", "Add a site" }, { "Done", "Done" }, { "Name", "Name" }, { "ID", "ID" }, { "Capacity", "Capacity" },
			{ "Insufficient_capacity", "Insufficient capacity" }, { "needed", "needed" }, { "available", "available" }, { "deficit", "deficit" }, { "Insufficient_capacities", "Insufficient capacities" },
			{ "Insufficient_capacities_in_destinations", "Insufficient capacities in the destinations!" }, { "illegal_string", "illegal string" }, { "illegal_number", "illegal number" }, { "must_be_positive_or_0", "must be positive or 0" },
			{ "must_be_positive", "must be positive" }, { "Illegal_capacity_for_site", "Illegal capacity for site" }, { "there_are_no_more_categories", "There are no more item categories!" }, { "no_more_categories", "No more categories!" },
			{ "Select_categories_to_add", "Select the categories to add" }, { "Confirm", "Confirm" }, { "wish_remove_site", "Do you really wish to remove the site" }, { "enter_sites_click_map", "Enter new site(s) by clicking on the map" },
			{ "take_sites_from", "take site(s) from" }, { "new_sites", "New site(s)" }, { "Select_the_sites", "Select the sites" }, { "Site_ID", "Site ID" }, { "Site_name", "Site name" }, { "Site_type", "Site type" },
			{ "Item_categories", "Item categories" }, { "New_site", "New site" }, { "Error", "Error" }, { "No_identifier_specified", "No identifier specified!" }, { "No_name_specified", "No name specified!" },
			{ "Duplicate_site_identifier", "Duplicate site identifier" }, { "The_site", "The site" }, { "has_same_identifier_as_site", "has the same identifier as the site" }, { "which_present_in_list", "which is already present in the list" },
			{ "wish_modify_identifier_", "Do you wish to modify the identifier and add the new site?" }, { "Identifier", "Identifier" }, { "in_source", "in source" }, { "delay_in_source", "delay in source" }, { "on_way", "on the way" },
			{ "in_destination", "in destination" }, { "in_unsuitable_vehicle", "in unsuitable vehicle" }, { "in_unsuitable_destination", "in unsuitable destination" }, { "Number", "Number" }, { "Time", "Time" },
			{ "number_of_objects", "number of objects" }, { "time_limit", "time limit" }, { "time_limit_not_specified", "the time limit is not specified" }, { "Error_in", "Error in" }, { "for_site", "for site" },
			{ "No_items_to_transport", "No items to transport!" }, { "Vehicles_for", "Vehicles for" }, { "Fleet_capacities_for", "Fleet capacities for" }, { "Destinations_for", "Destinations for" },
			{ "Capacities_in_destinations_for", "Capacities in destinations for" }, { "Unused", "Unused" }, { "Partly_unused", "Partly unused" }, { "Partly_filled", "Partly filled" }, { "Full", "Full" }, { "Overloaded", "Overloaded" },
			{ "Unsuitable", "Unsuitable" }, { "Idle", "Idle" }, { "Moving_without_load", "Moving without load" }, { "Loaded", "Loaded" }, { "capacity_in_free_vehicles", "capacity in free vehicles" },
			{ "free_capacity_in_used_vehicles", "free capacity in used vehicles" }, { "used_capacity_in_used_vehicles", "used capacity in used vehicles" }, { "overload", "overload" },
			{ "used_capacity_in_unsuitable_vehicles", "used capacity in unsuitable vehicles" }, { "source_UI_title", "Items in source locations" },
			{ "Items_in_sources_and_time_limits", "Items in the source sites and their time limits, in minutes" }, { "vehicles_UI_title", "Vehicles suitable for the evacuation" }, { "Sum_of_capacities", "Sum of capacities" },
			{ "Available_vehicles_and_their_locations", "Available vehicles and their locations" }, { "add_vehicle_type", "Add a vehicle type" }, { "Type_of_vehicle", "Type of vehicle" }, { "Ready_time", "Ready time" },
			{ "Illegal_number_of_vehicles_in_site", "Illegal number of vehicles in site" }, { "Illegal_readiness_time_in_site", "Illegal readiness time in site" }, { "No_vehicles_for_transporting", "No vehicles for transporting" },
			{ "any_items", "any items" }, { "No_more_types_", "No more types!" }, { "There_are_no_more_vehicle_types_", "There are no more vehicle types!" }, { "Select_the_types_to_add", "Select the types to add" },
			{ "Vehicle_types", "Vehicle types" }, { "Types_of_vehicles", "Types of vehicles" }, { "Enter_point", "Enter a point" }, { "Enter_point_on_map", "Enter a point on the map" },
			{ "scheduler_working", "The scheduler is working. You will be notified about its progress and results." }, { "Specify_file_with_orders", "Specify the file with the orders" }, { "Common_time_limit", "Common time limit (minutes)" },
			{ "File_name", "File name" }, { "Vehicle", "Vehicle" }, { "Type", "Type" }, { "Initial_position", "Initial position" }, { "latitude", "latitude" }, { "longitude", "longitude" }, { "Trip_N", "Trip N" }, { "Time", "Time" },
			{ "Load", "Load" }, { "Amount", "Amount" }, { "Origin", "Origin" }, { "Identifier", "Identifier" }, { "Destination", "Destination" }, { "End_time", "End time" }, { "Select_item_categories", "Select item categories" },
			{ "Fleet_activities", "Fleet activities" }, { "Items_state", "Items state" }, { "Destinations_use", "Destinations use" }, { "Fleet_use", "Fleet use" }, { "Number_of_trips", "Number of trips" },
			{ "Number_of_trips_with_load", "Number of trips with load" }, { "Number_of_trips_without_load", "Number of trips without load" }, { "Number_of_people", "Number of people" },
			{ "Number_of_different_vehicles", "Number of different vehicles" }, { "Number_of_different_vehicles_with_load", "Number of different vehicles with load" }, { "", "" } };

	public Object[][] getContents() {
		return contents;
	}
}
