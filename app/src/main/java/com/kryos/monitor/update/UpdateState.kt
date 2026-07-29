package com.kryos.monitor.update

enum class UpdateState {
    IDLE,           
    DOWNLOADING,    
    INSTALLING,     
    RESTARTING,     
    COMPLETED,      
    FAILED,         
    CANCELLED       
}
