**Design Choices**

Files Class and Array
- originally stored files in an array list
    - found out that entity info response had its own definition of the files in a hashmap
- defined new hashmap in create device and create satellite 
    - then changed it to declaring a new hashmap in the instance creation in the class file
        - this prevented clutteredness and improved efficiency of code design.

Devices and Satellites
- Originally made a simple create function ie. Object objName = new Obj();
    - because there is multiple types of devices and satellites, its much better that i use something like switch cases to create different types of satellites and devices
    - also used switch cases for the different velocity and range of each satellite and device
- Received feedback that I should include my relays and teleporting movement in their respective classes 
    - Made it much easier to implement both sets of code
    - Thinking of doing the same for get satellite, device by id and get file by filename 

Integrating a more efficient hashmap system progress (Final week):
- Initially, the thought of overhauling my entire system for managing my entities such as the array list i made for satellites devices and file was soooo daunting. 
    - However, the actual integration was alot smoother once I revised my knowledge on mapping and using hashmaps
- I struggled with properly incorporating error handling mechanisms within the codebase. Throwing exceptions and ensuring that they were caught and handled appropriately proved to be a complex endeavor. It took some time to but was enough for me to finish stage 2 tests.


To-Do:
- Need to include comments using java doc


UNANSWERED Questions:


ANSWERED Questions: 
- Look over the communicableEntitiesInRange() function. Need help on why the tests aren't passing
Problem solved. came across issue of satellite including itself in range:
    - can either exclude or remove from array list
    - because of the idea relay because they're like a signal booster so all extra device would have to be include, so you shouldn't exclude rather just remove the id satellite itself

- Why does it say You should NOT store any response objects (i.e. FileInfoResponse or EntityInfoResponse) in your classes, pull the information out of those into ArrayLists/Fields/Classes. You can create it them in the classes though.
    - does this mean I should be using the hashmap definition of files in my Device and Satellites class
    ask tutor if this is ok

- How do I run the simulation function
I have to make the simulation function

- Do I have to make error handling exceptions for things like satellite/device not found, I've seen for files but not ther other 2?
No I do not have to error handle misinput on exceptions and system failures for Files
    - Where can I find this on the spec?
    I can find this in other requirements on the spec

- Is the UML supposed to be what your code looks like at the end or a plan for what it should look like at the end of Task 1?
It's supposed to look like the model that you have at the end of the code

- Do I have to include Design by Contract stuff into my code, like pre and post conditions? Coz i didn't really see any pre and post conditions in the specs
NO. Design by Contract is  not something you need for Assignment 1.




