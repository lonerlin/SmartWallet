
#include <Wire.h>

#include <SoftwareSerial.h>


#define TOUCHKEY    12
#define RLED 2
#define GLED 3
#define BUZZER 4

const int analogInPin = A5;


bool IsWorking=false;
bool IsAlarm=false;
bool searching=false;
double AlarmDelayTime=300;
double LoseTrackTime=0;
double AlarmTime=0;



boolean key_flag;

SoftwareSerial mySerial( 10,11); // RX, TX




void checkTouchButton()
{
     if (digitalRead(TOUCHKEY) == HIGH) //按键按下
    {
        if(!key_flag)AlarmTime=millis();
        key_flag = true;
       
    }
    else if (key_flag == true) //按键被放开，但按键事件还未被处理
    {
        long interval=millis()-AlarmTime;
       // Serial.println("touch");
        
        if(interval >6000)
        {
          IsWorking=false;
          searching=false;
          IsAlarm=false;
        }else if(  (interval>2500) && (!IsAlarm))
        {
            // Serial.println("IsAlarm");
             IsAlarm=true;  
             
        }else if(interval>1000 && IsAlarm)
        {
            IsAlarm=false;
        } 
        if(searching)searching=false;
        key_flag = false;
    }
    else //按键被放开，且按键触发的事件也已经被处理
    {
       
    }
    
}
void SearchWallet(bool IsSearch)
{
    if(IsSearch)
    {
       //Serial.println("IsSearch");
       digitalWrite(RLED, HIGH);
       digitalWrite(GLED,LOW); 
      int OldTime = 0; 
        for (int i = 450; i < 800; i+=1)
        {
            tone(BUZZER, i);
            if(millis() - OldTime >40)
            {
              digitalWrite(RLED, !digitalRead(RLED));
              digitalWrite(GLED,!digitalRead(GLED));
              OldTime=millis();
            }
        }
        for (int i = 800; i > 450; i-=1) 
        {
          tone(BUZZER, i);
          if(millis() - OldTime >40)
          {
             digitalWrite(RLED, !digitalRead(RLED));
             digitalWrite(GLED,!digitalRead(GLED));
              OldTime=millis();
          }
        }
        
    }

}

void closeWarning()
{
    digitalWrite(RLED, LOW);
    digitalWrite(GLED,LOW); 
    noTone(BUZZER);
}

void execute(int command)
{
  //Serial.println(command);
  if(command>=0 && command<7)
  {  
    switch(command){
            case 0:
              IsWorking=true;
              break;
           case 6:
             IsWorking=false;
             break;
           case 1:
             mySerial.write(1);
             break;
           case 2:
             IsAlarm=true;
             break;
           case 3:
            IsAlarm=false;
            break;
           case 4:
            searching=true;
            break;
           case 5:
            searching=false;
            break;
           default:
             break;         
            
          }
  }
}

bool getLightLevel()
{
      double ave=0;
      for(int i=0;i<4;i++)
      {
          ave+=analogRead(analogInPin);
          delay(10);
      }
      ave=ave/4;
     // Serial.print("ave:");
     // Serial.println(ave);
      if(ave>300)
      {
          return true;
        }else
        {
          return false;
          }
}


void setup()
{
  pinMode(TOUCHKEY, INPUT);
  pinMode(RLED,OUTPUT);
  pinMode(GLED,OUTPUT);
  pinMode(BUZZER,OUTPUT);
  key_flag = false;
  Serial.begin(9600);
  mySerial.begin(9600);
  
  LoseTrackTime=millis();
}



void loop()
{
    //检测端口
    if(mySerial.available())
    {
       int command=mySerial.read();
       execute(command); 
       LoseTrackTime=millis();
    }
    //收不到信息发送警告 
    if(IsWorking)
    {
        if((millis()-LoseTrackTime)>4000)
        {
            SearchWallet(true);
           // Serial.println("SearchWallet");
        }
    }

    //检测触摸按钮 
     checkTouchButton();

    //收到查找信号，直接响警报 
    if(searching)
    {
      SearchWallet(true);  
    }
    else
    {
      closeWarning();  
    }
    
   
  
    if(IsAlarm && getLightLevel())
    {
        SearchWallet(true);
        
        mySerial.write(2);   //向手机发送警报  
    }
    else
    {
      closeWarning();  
    }
}



