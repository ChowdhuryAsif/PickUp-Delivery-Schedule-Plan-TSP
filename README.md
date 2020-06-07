# PickUp-Delivery-Schedule-Plan-TSP
## A pick-up/delivery schedule planning using Genetic Algorithm (Artificial Intelligence)

Pickup/Delivery Schedule is suppose to approximate a best path plan as we cannot find the best path with 100% accuaracy.
Clients provide order with locations in terms of longitude and latitude to pick their packages within a window time. If the pick-up man cannot take their packages in window or in duty time he/she may encountered some penalty.

For more clarification about the problem please read problem description.

## To clarify how the program works input and output is given below.

## INPUT

```Java
//Number of input
//longitude latitude startTime windowTime
8
5.00 15.00 630 1800
2.00 3.00 1300 1530
7.00 47.00 900 1000
35.00 40.00 1045 1820
77.00 85.00 1500 1830
31.00 43.00 815 1300
19.00 51.00 605 0740
21.00 16.00 600 1800
```
## OUTPUT

```Java
Head Office Time: 600
Longitude: 21.00, Latitude: 16.00 Time: 802
Longitude: 19.00, Latitude: 51.00 Time: 846
Longitude: 7.00, Latitude: 47.00 Time: 1047
Longitude: 5.00, Latitude: 15.00 Time: 1240
Longitude: 31.00, Latitude: 43.00 Time: 1321
Longitude: 2.00, Latitude: 3.00 Time: 1452
Longitude: 35.00, Latitude: 40.00 Time: 1553
Longitude: 77.00, Latitude: 85.00 Can't Provide Service.
Head Office Time: 1709
Penalty: 10083
```
