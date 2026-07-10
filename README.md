0. New terminal  
C:\Users\haads  

1. cd Cursor | cd vsCode  
PS C:\Users\haads\cursor>  

2. git clone https://github.com/HaadLIT/JavaAppBoilerplate.git LeetCode2  
adding "LeetCode2" assigns new name to clone  

3. Open project @path  
PS C:\Users\haads\Cursor\LeetCode2>   

4. View current orign, remove + delete current, view again  
git remote -v (View)  
Remove-Item -Recurse -Force .git (Remove current origin + delte git history)  
git remote -v (Verify)  

5. Next init the new repo  
git init  
git remote add origin https://github.com/HaadLIT/LeetCode2.git  
git add .  
git commit -m "Intial commit"  
git branch -M main  
git push -u origin main  

***
Push code to new repo  
***
git add .  
git commit -m "comments"  
git push  


***
Create runnable .jar  
***
1.      
Add to build.gradle.kts:   
   
tasks.jar {   
    manifest {   
        attributes["Main-Class"] = "com.haadlit_sp.Main"   
    }   
}   

2.   
Run:
.\gradlew.bat jar   ( to produce build\libs\NetTracker-1.0-SNAPSHOT.jar)   

Make sure to rename rootProject.name = "NetTracker" (to currnet project name)   

***
Add to start menu     
***
0. Go to start up programs folder   
1. New ShortCut -> 
Location = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\bin\javaw.exe" -jar "C:\Users\haads\vsCode\Net\build\libs\NetTracker-1.0-SNAPSHOT.jar"   




