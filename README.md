# GurionRock Pro Max Ultra Over 9000 Vacuum Robot Perception & Mapping Simulation

A Java-based microservices framework and robotics simulation for processing sensor data (camera, LiDAR, IMU/GPS) and building a global map via Fusion-SLAM.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Features](#features)
3. [Requirements](#requirements)
4. [Getting Started](#getting-started)

   * [Clone & Build](#clone--build)
   * [Running the Simulation](#running-the-simulation)
5. [Configuration & Input Files](#configuration--input-files)
6. [Output](#output)
7. [Project Structure](#project-structure)
8. [Testing](#testing)
9. [Contributing](#contributing)

---

## Project Overview

This assignment implements a custom microservices framework in Java (using threads, synchronization, and callbacks) and builds a perception-and-mapping system for a vacuum-mopping robot. The simulation:

* Broadcasts global clock ticks (`TimeService`)
* Reads camera, LiDAR and pose data from JSON files
* Distributes `DetectObjectsEvent` and `TrackedObjectsEvent` among sensor workers
* Integrates data in `FusionSlamService` to maintain a global landmark map
* Handles sensor termination or crash broadcasts

## Features

* **Microservices Framework**

  * Thread-safe `MessageBus` singleton with event/broadcast dispatch and round‑robin load balancing
  * Abstract `MicroService` base class with message loop and subscription callbacks
* **Concurrency Constructs**

  * Custom `Future<T>` implementation for event result handling
  * Use of `synchronized`, concurrent queues, and minimal blocking
* **Sensor Services**

  * `TimeService`, `CameraService`, `LiDarWorkerService`, `FusionSlamService`, `PoseService`
  * Support for termination and crash broadcasts
* **Simulation I/O**

  * Input: JSON files for configuration, pose, LiDAR data, and camera detections
  * Output: `output_file.json` with statistics and world map

## Requirements

* Java 8 or later
* Maven 3.x
* JSON parsing library (Gson recommended)

## Getting Started

### Clone & Build

```bash
# Clone your repository
git clone https://github.com/<yourusername>/GurionRock-SPL225.git
cd GurionRock-SPL225
# Build with Maven
mvn clean package
```

### Running the Simulation

```bash
# Example: run with a configuration JSON
java -jar target/SPL225-Assignment2.jar path/to/configuration.json
```

* Replace `path/to/configuration.json` with your config file path.
* The simulation will run for the configured duration or until sensors terminate/crash.

## Configuration & Input Files

The root-level configuration JSON specifies:

* **Cameras**: `id`, `frequency`, `camera_datas_path`, `camera_key`
* **LiDarWorkers**: `id`, `frequency`, `lidars_data_path`
* **Pose**: `poseJsonFile`
* **Timing**: `TickTime`, `Duration`

Additional JSON files define per-tick:

* Camera detections (`detectedObjects`)
* LiDAR point clouds (`cloudPoints`)
* Robot poses (`x`, `y`, `yaw`)

## Output

After completion (or error), the simulator writes `output_file.json` alongside the config, containing:

* **statistics**: `systemRuntime`, `numDetectedObjects`, `numTrackedObjects`, `numLandmarks`
* **landMarks**: array of `{ id, description, coordinates }`
* **error** section\*\* (if a sensor crashed): includes faulty sensor, last frames, poses, and partial statistics

## Project Structure

```
├── pom.xml
├── src
│   ├── main
│   │   └── java
│   │       └── bgu.spl.mics
│   │           ├── framework      # MessageBusImpl, MicroService, Future
│   │           └── services       # TimeService, CameraService, LiDarWorkerService, PoseService, FusionSlamService
│   └── test
│       └── java                   # JUnit tests for Future, MessageBus, core services
└── README.md
```

## Testing

Run unit tests with:

```bash
mvn test
```

* Ensure coverage for your `Future<T>`, `MessageBusImpl`, and at least one sensor service.

## Contributing

Feel free to fork, experiment, and submit pull requests! For questions or issues, open an issue in this repository.

---

**Author:** Tomer Roemy
**Course:** SPL 225 – Concurrent Programming (Assignment 2)
=======

>>>>>>> master
