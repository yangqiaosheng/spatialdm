package spade.analysis.tools;

/**
* This interface is to be implemented by tools that allow only a single
* instance to be created during a session. The instance is created when the
* tool is requested for the first time. Next time the ToolKeeper must find the
* previously created instance.
* No methods, constants, or variables are defined in this interface.
*/
public interface SingleInstanceTool {
}