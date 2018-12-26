# ScaleImageApplication
支持手势缩放、双击缩放、移动的ImageView

 	Add it in your root build.gradle at the end of repositories:
 	 allprojects {
		repositories {
		...
		maven { url 'https://jitpack.io' }
		}
	}
  
	Step 2. Add the dependency
  	dependencies {
		 implementation 'com.github.sidan26:ScaleImageApplication:Tag'
  	}
  
 	<com.example.tyr.scaleimageapplication.ZoomImageView
        	android:layout_width="match_parent"
        	android:layout_height="match_parent"
        	android:src="@drawable/c"
        	android:scaleType="matrix"/>
