# Set_Card_Game---SPL_Assignment---JAVA---Grade-100
This is an assignment I was given during "SPL" course in univesity, that utilizes many synchronization mechanisems in Java.

This project is supposed to be a "Set" game where it is possible to choose the number of human participants and the number of computer participants. Also, it is possible to configure the speed of the computer participants. The challenge was to make sure that the game plays smoothly without bugs even when played with multi-computer participants (each participant is represented by a thread of it's own) at high game speeds.

WHAT I LIKED ABOUT THIS ASSIGNMENT

What I liked about this assignment, is that it geniuenly taught me how to use Java's synchronization mechanisms. There are "key holder" objects that are used to manage synchronziation, as well as atomic queues and stacks, compareAndSet method, and volatile variables. I think that utilizing synchronization is a sort of art.

HOW TO RUN

The main function in the java code is in: src/main/java/Main.java. If you open the project with VScode, you can run it from there. To configure the settings of the game, such as the computer-players' spee
