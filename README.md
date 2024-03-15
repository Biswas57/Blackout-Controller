# Blackout-Controller | Communications Satellite Simulator
## Overview
Blackout Communication Simulation is a comprehensive software framework designed to model and simulate the dynamic interactions between earth-based devices and satellites within a controlled environment. It enables the simulation of satellite movements, device connectivity, and file transfer protocols, mirroring real-world satellite communication networks. The project offers a unique insight into the challenges and solutions of managing a space-based communication network.

- Utilized Java to design a satellite communication simulator for a futuristic society, using strict OOP design principles.
- Crafted a dynamic backend model to manage file uploads/downloads, satellite movement, and device interactions.
- Integrated a frontend interface for the satellite simulator, utilizing RESTful API endpoints to handle various HTTP requests.

## Key Features
- **Dynamic Entity Modeling:** Represents various types of satellites (standard, teleporting, relay) and devices (handheld, laptop, desktop) with unique characteristics.
- **File Transfer Simulation:** Manages the complexity of sending and receiving files, considering bandwidth limitations, storage capacity, and transfer rates.
- **Movement and Range Simulation:** Tracks the movement of satellites and calculates the visibility and range between devices and satellites, incorporating real-world physics.
- **Network State Visualization:** Provides visual tools to observe the satellite network's state over time, aiding in analysis and understanding.

## Components
- **Device and Satellite Modeling:** Java classes encapsulate the behavior and properties of devices and satellites, offering a robust foundation for simulation.
- **File Management System:** A sophisticated system for file creation, storage, deletion, and transfer, simulating real-world network traffic.
- **Simulation Engine:** Drives the core simulation process, including the movement of satellites, file transfers, and connectivity updates.
- **Visualization Tools:** A suite of visualization options to display the network state, device connections, and file transfer status, enhancing user experience and comprehension.

## Getting Started
### Prerequisites
- Java JDK 11 or higher
- A modern IDE (e.g., IntelliJ IDEA, Eclipse)
### Setup and Installation
1. Clone the repository to your local machine:
`git clone https://github.com/your-username/Blackout-Simulation.git`
2. Navigate to the project directory and compile the Java files.
### Running the Simulation
Execute the main method in `app/src/main/java/app.java` to start the simulation. Customize simulation parameters as needed to explore different scenarios.

