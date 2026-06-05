# TransitManager 2.0.0 beta
This is a server-side addon for **Minecraft Transit Railway 4.0** Mod, adding lower-level commands that allows you to have better control of traffic in MTR.

Available for Minecraft Fabric 1.20.4, port for older versions is planned in the future.

## Commands
| Command                                                         | Description                                                                                                                                                                                                         | Permission Level |
|-----------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| /mtr generatePath <Depot Name><br>/mtr clearTrains <Depot Name> | Refresh a depot's path, or clear all trains of a depot                                                                                                                                                              | OP Level 2       |
| /mtrpath status<br>/mtrpath abort <Depot Name>                  | Check, refresh and abort MTR Path generations.<br><br>- status: Show how many depots are currently regenerating path<br>- abort: Terminate the depot's path generation. (If it is still generating)                 | OP Level 2       |
| /platform                                                       | Give you the information of the nearest platform, including dwell,<br>transport type and route that goes via that platform.                                                                                         | OP Level 2       |
| /train                                                          | Provide the information of the nearest train, such as the train type, it's depot and siding, destination, dwell and more.                                                                                           | OP Level 2       |
| /train deploy <Departure Index>                                 | This will find the nearest train to you, and manually deploy the train<br>if it is parked inside a depot, regardless of timetable and deploy order.                                                                 | OP Level 2       |
| /train clear                                                    | This clears all trains from the siding of the nearest train. If that siding has multiple trains, all of them will be cleared.                                                                                       | OP Level 2       |
| /train skipDwell                                                | This skips the dwell time for the nearest train.<br>If the train is boarding, it will immediately depart and the door will close.<br>Otherwise, it will skip the dwell time in the next stop like a Turn Back Rail. | OP Level 2       |
| /warpsta <Station Name>                                         | Warp to the specified station's midpoint. You'll be teleported to the nearest non-suffocatable Y level of the midpoint of the station area.                                                                         | OP Level 2       |
| /warpdepot <Depot Name>                                         | Warp to the specified depot's midpoint. You'll be teleported to the nearest non-suffocatable Y level of the midpoint of the station area.                                                                           | OP Level 2       |

## Config
The config file are automatically generated in `config/mtrate/server.toml` once the server starts up.

| Config Key                | Default Value | Description                                                                                                                                                                                                                |
|---------------------------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| shearPSDOpLevel           | 0             | This defines the OP Level required to shear the top part of the Platform Screen Doors in the MTR Mod. Server owners may want to bump this up to OP Level 1/2 to prevent non-builders from modifying Platform Screen Doors. |

## Bugs/Suggestion
If you have any questions or suggestions, feel free to open an GitHub issue [here](https://github.com/DistrictOfJoban/TransitManager/issues) :)

## License
This project is licensed under the MIT License.