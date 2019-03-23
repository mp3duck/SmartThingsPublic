definition(
    name: "Mitsubishi Connect",
    namespace: "DannyW",
    author: "Danny Winbourne",
    description: "Connect your Mitsubishi aircon to  SmartThings.",
    category: "Green Living",
    iconUrl: "https://i.ibb.co/pPBf9PS/mellogo.png",
    iconX2Url: "https://i.ibb.co/pPBf9PS/mellogo.png",
    iconX3Url: "https://i.ibb.co/pPBf9PS/mellogo.png",
    singleInstance: true)

{
    appSetting "apikey"
    appSetting "buildingid"
}

preferences {
   // page(name: "SelectAPIKey", title: "API Key", content: "setAPIKey", nextPage: "logondetails", install: false, uninstall: true)
    page(name: "logondetails", title: "Logon details", content:"setLogon", nextPage: "deviceList", install:false, uninstall: true)
    page(name: "deviceList", title: "melcloud", content:"melcloudList", install:true, uninstall: true)
}

def getServerUrl() { "https://app.melcloud.com" }
def getapikey() { state.apikey }

public static String version() { return "SmartThingsv1.6" }

def setAPIKey()
{
    log.trace "XX setAPIKey()"
    
    def pod = appSettings.apikey

    def p = dynamicPage(name: "SelectAPIKey", title: "Enter your API Key", uninstall: true) {
        section(""){
            paragraph "Please enter your API Key"
            input(name: "apiKeyx", title:"", type: "text", required:true, multiple:false, description: "", defaultValue: pod)
            //  input(name: "melusername", title:"Username", type: "text", required:true, multiple:false, description: "")
            // input(name: "melpassword", title:"Password", type: "password", required:true, multiple:false, description: "")
        }
    }
    return p
}

def setLogon()
{
    log.trace "setLogon()"
    
    //  def pod = appSettings.apikey

    def p = dynamicPage(name: "logondetails", title: "Enter your logon details", uninstall: true) {
        section(""){
            paragraph "Please enter your logon details"
            input(name: "melusername", title:"Username", type: "text", required:true, multiple:false, description: "", defaultValue: "21clintonavenue@gmail.com")
            input(name: "melpassword", title:"Password", type: "password", required:true, multiple:false, description: "", defaultValue: "144FNashton")
        }
    }
    return p
}

def melcloudList()
{
    def stats = getMelList()

    def p = dynamicPage(name: "deviceList", title: "Select Your Sensibo Pod", uninstall: true) {
           section("Refresh") {
        	input(name:"refreshinminutes", title: "Refresh rates in minutes", type: "enum", required:false, multiple: false, options: ["1", "5", "10","15","30"])
        }
        section(""){
        
            paragraph "Tap below to see the list of Sensibo Pods available in your Sensibo account and select the ones you want to connect to SmartThings."
            input(name: "SelectedSensiboPods", title:"Pods", type: "enum", required:true, multiple:true, description: "Tap to choose", metadata:[values:stats])
        }
    }
}

def getMelList()
{
    loginToTheCloud()
    log.trace "getMelList called - key = " + getapikey()
    log.trace "state.apikey " + state.apikey
    def deviceListParams = [
        uri: "${getServerUrl()}",
        path: "/Mitsubishi.Wifi.Client/User/ListDevices",
        requestContentType: "application/json",
        headers: ["X-MitsContextKey":"${getapikey()}"]]
    //query: [X-MitsContextKey:$"{getapikey()}" ]]

    def pods = [:]

    try {
        httpGet(deviceListParams) { resp ->
    	if(resp.status == 200) {
                log.trace "resp.data[0].ID " + resp.data[0].ID
                state.buildingid = resp.data[0].ID
                //  log.trace "Building ID is " +buildingID()
                //  log.debug "xx2 got response"
                //  def results = new groovy.json.JsonSlurper().parseText("{resp[0]}")
                //   log.debug "qq got response"
                // log.debug "No. Floors - " + resp.data.Structure.Floors[0];
                // log.debug "No. Floors - " + results.Structure.Floors.size();

                //log.debug "Starting floor 0 - " + resp.data[0].Structure.Floors.size();
                //log.debug "Starting floor 0 - " + resp.data.Structure.Floors[1].Areas[1].Name;
                // log.debug "Starting floor 1 - " + resp.data;
                //log.debug "Starting floor 1 - " + resp.data.Structure.Floors[1].Areas[1].Name;
                //log.debug "Starting floor 1 - " + resp.data.Structure.Floors;

                for (int i = 0; i < resp.data.Structure.Floors.size(); i++) {
                    //  log.debug "floor!"
                }

                resp.data[0].Structure.Floors.each { pod ->
                    //log.debug "floor x "
                    //log.debug "Looking at floor " + pod.Name

                    pod.Areas.each{ area ->
                        // log.debug " -- Looking at area " + area.Name

                        area.Devices.each{dev ->
                            //	log.debug " -- Device: " + dev.DeviceName

                            def key = dev.DeviceID + "id"
                            def value = dev.DeviceName

                            pods[key] = value
                        }
                    }
                }
            }
        }
    }
    catch (Exception e) {
        log.debug "XXException Get Json: " + e
        //debugEvent ("Exception get JSON: " + e)
    }

    return pods
}

def installed() {
    log.trace "Installed() called with settings: ${settings}"

    //state.lastTemperaturePush = null
    // state.lastHumidityPush = null
    loginToTheCloud()
    initialize()

    def d = getChildDevices()



    log.debug "Configured health checkInterval when installed()"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: true)

    //subscribe(d,"temperatureUnit",eTempUnitHandler)

    // if (sendPushNotif) { 
    subscribe(d, "temperature", eTemperatureHandler)
    // subscribe(d, "humidity", eHumidityHandler)
    //}
}

def getChildNamespace() { "DannyW" }
def getChildTypeName() { "MelPod" }

def loginToTheCloud()
{
    ///Mitsubishi.Wifi.Client/Login/ClientLogin
    log.trace "Loggin in ..."
    def cmdParams = [
        uri: "${getServerUrl()}",
        path: "/Mitsubishi.Wifi.Client/Login/ClientLogin",
        headers: ["Content-Type": "application/json"],
        body: '{"Email": "' + melusername + '", "Password": "' + melpassword + '", "Language": 0, "AppVersion": "1.17.5.0"}']
    log.trace "cmdParams: " + cmdParams

    try {
        httpPost(cmdParams) { resp ->
	   		if(resp.status == 200) {
                log.trace "login succcess - " + resp.data.ErrorId
                // appSettings.apikey = resp.data.LoginData.ContextKey
                // apikey = resp.data.LoginData.ContextKey
                state.apikey = resp.data.LoginData.ContextKey
                log.trace "state.apikey  xxx "  + state.apikey
            }
        }
    }
    catch (Exception e) {
        log.debug "Exception loggin in: " + e
        debugEvent("Exception loggin in: " + e)
        return false
    }
}

def initialize() {
    log.trace "initialize() called"
    log.trace "key "+ getapikey()
    log.trace "SelectedSensiboPods: " + getMelList()
    def devices = SelectedSensiboPods.collect { dni ->
        log.debug "Wxxx"
        log.debug dni
        def d = getChildDevice(dni)

        if (!d) {
            log.debug "in d - dni is - " + dni
            //

            def name = getMelList().find({ key, value -> key == dni })

            log.debug "name is: " + name
            log.debug "Pod : ${name.value} - Hub : ${location.hubs[0].name} - Type : " + getChildTypeName() + " - Namespace : " + getChildNamespace()

            d = addChildDevice(getChildNamespace(), getChildTypeName(), dni, location.hubs[0].id, [
                "label" : "Pod ${name.value}",
                "name" : "Pod ${name.value}"
            ])
            d.setIcon("on", "on", "https://i.ibb.co/8DcVYgF/zenicon.png")
            d.setIcon("off", "on", "https://i.ibb.co/8DcVYgF/zenicon.png")
            d.save()

        }
        return d
    }

    log.trace "created ${devices.size()} Sensibo Pod"

    def delete
	// Delete any that are no longer in settings
	if(!SelectedSensiboPods) {
        log.debug "delete Sensibo"
        delete = getChildDevices()
    }
    else {
        delete = getChildDevices().findAll { !SelectedSensiboPods.contains(it.deviceNetworkId) }
    }

    log.trace "deleting ${delete.size()} Sensibo"
    
    delete.each { log.trace "deleteing device ID : " + it.deviceNetworkId }

    /* try
     {
     delete.each { deleteChildDevice(it.deviceNetworkId) }
     }
                     catch(Exception e)
     {
         log.debug "___exception deleting children: " + e
         debugEvent ("${e}")
     }*/
    def PodList = getChildDevices()

    pollHandler()

    refreshDevices()

    if (refreshinminutes == "1")
        runEvery1Minute("refreshDevices")
    else if (refreshinminutes == "5")
        runEvery5Minutes("refreshDevices")
    else if (refreshinminutes == "10")
        runEvery10Minutes("refreshDevices")
    else if (refreshinminutes == "15")
        runEvery15Minutes("refreshDevices")
    else if (refreshinminutes == "30")
        runEvery30Minutes("refreshDevices")
    else
        runEvery10Minutes("refreshDevices")

}


def pollHandler() {

    debugEvent("in Poll() method.")

    // Hit the Sensibo API for update on all the Pod

    def PodList = getChildDevices()

    log.debug PodList
    PodList.each { 
        log.debug "polling " + it.deviceNetworkId
        pollChildren(it.deviceNetworkId)
    }

    state.sensibo.each {stat ->

        def dni = stat.key

        log.debug("DNI = ${dni}")
        debugEvent("DNI = ${dni}")

        def d = getChildDevice(dni)

        if (d) {
            log.debug("Found Child Device.")
            debugEvent("Found Child Device.")
            debugEvent("Event Data before generate event call = ${stat}")
            log.debug state.sensibo[dni]
            d.generateEvent(state.sensibo[dni].data)
        }
    }
}

def refreshDevices() {
    log.trace "refreshDevices() called"
    def devices = getChildDevices()
    devices.each { d ->
        log.debug "Calling refresh() on device: ${d.id}"
        
        d.refresh()
    }
}

def refresh() {
    log.trace "refresh() called with rate of "// + refreshinminutes + " minutes"

    unschedule()

    refreshDevices()
}

def debugEvent(message, displayEvent = false) {

    def results = [
        name: "appdebug",
        descriptionText: message,
        displayed: displayEvent
    ]
    log.debug "Generating AppDebug Event: ${results}"
    sendEvent(results)

}

def buildingID()
{
    return 82830
}

def getID(theID)
{
    return theID.replace("id", "")
}

def pollChildren(PodUid)
{

    log.trace "pollChildren() called"
    
    def thermostatIdsString = PodUid
    def melID = getID(PodUid)

    def dni = thermostatIdsString
    log.trace "polling children: $thermostatIdsString"
    log.trace "API KEY " + state.apikey
    def pollParams = [
        uri: "${getServerUrl()}",
        path: "/Mitsubishi.Wifi.Client/Device/Get",
        requestContentType: "application/json",
        query: [id:"${melID}", buildingID:"${state.buildingid}"],
        headers: ["X-MitsContextKey":"${getapikey()}"]]


    try {
        httpGet(pollParams) { resp ->

			if (resp.data) {
                debugEvent("Response from Sensibo GET = ${resp.data}")
                debugEvent("Response Status = ${resp.status}")
            }
            def stemp = resp.data.RoomTemperature ? resp.data.RoomTemperature.toDouble().round(1) : 0
            def ttemp = resp.data.SetTemperature ? resp.data.SetTemperature.toDouble().round(1) : 0
            log.trace "stemp: " + stemp
            // log.trace "LastCommunication is: " + res
            log.trace "power is: " + resp.data.Power //== "true" ? "on" : "off"
            log.trace "resp.data.OperationMode: " + resp.data.OperationMode
            log.trace "resp.data.VaneHorizontal: "+ resp.data.VaneHorizontal
            def collector = [:]
            //log.trace collector
            def data = [
            	temperature: stemp,
                targetTemperature: ttemp,
                thermostatFanMode: ttemp,
                coolingSetpoint: ttemp,
                heatingSetpoint: ttemp,
                thermostatSetpoint: ttemp,
                temperatureUnit : ttemp,
                on: resp.data.Power ? "on" : "off",
                switch: resp.data.Power ? "on" : "off",
                LastCommunication: resp.data.LastCommunication,
                NextCommunication: resp.data.NextCommunication,
                thermostatOperatingState: resp.data.Power ? getOperatingState(resp.data.OperationMode) : "xidle",
                VaneHorizontalMode: resp.data.VaneHorizontal,
                VaneVertical: resp.data.VaneVertical,
                SetFanSpeed: resp.data.SetFanSpeed]
                        
            collector[dni] = [data:data]
            state.sensibo = collector
        }
    }
    catch (Exception e) {
        log.debug "___exception polling children: " + e
        debugEvent("${e}")
    }

    //https://app.melcloud.com/Mitsubishi.Wifi.Client/Device/Get?id=130513&buildingID=82830 



    // return [:]

}

def getOperatingState(int stateID){
    //OperationMode
    switch (stateID) {
        case 3:
            return "cooling"
        case 2:
            return "Dry"
        case 7:
            return "fan only"
        case 8:
            return "auto"
        case 1:
            return "heating"




    }

    return "yidle"
}

def updated() {
    log.trace "Updated() called with settings: ${settings}"
    
    unschedule()
    unsubscribe()

    state.lastTemperaturePush = null
    state.lastHumidityPush = null
    loginToTheCloud()

    // initialize()

    def d = getChildDevices()

    log.debug "Configured health checkInterval when installed()"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: true)
    subscribe(d, "temperature", eTemperatureHandler)
}


def eTemperatureHandler(evt){
    def currentTemperature = evt.device.currentState("temperature").value
    log.trace "current temp: " + currentTemperature
}

def pollChild(child)
{
    pollChildren(child.device.deviceNetworkId)
    log.trace "pollChild() 2xx called"
    log.trace "state.Xbuildingid  " + state.buildingid
    log.trace "appSettings.buildingID  " + buildingID()
    debugEvent("poll child 2")
    def tData = state.sensibo[child.device.deviceNetworkId]
    log.trace "tdata 1: " + tData.data
    def currentTime = new Date().time
    tData.updated = currentTime
    log.trace "tdata 2: " + tData
    return tData.data

}

def setACStates(child, String PodUid, power, mode, targetTemperature, fanLevel, swingM, sUnit, int effectiveFlag)
{
    log.trace "setACStates() called for $PodUid, with target temp of $targetTemperature"
    pollChildren(PodUid)
    def tData = state.sensibo[PodUid]
    log.trace "tData: " + tData

    def jsonRequestBody = '{"EffectiveFlags":' + effectiveFlag + ',"DeviceID":' + getID(PodUid)
    jsonRequestBody += ', "SetTemperature":' + targetTemperature
    jsonRequestBody += ', "Power":' + power
    switch (effectiveFlag) {
        case 4:
            //	jsonRequestBody += ', "SetTemperature":' + targetTemperature
            break

        case 1:
            //	jsonRequestBody += ', "Power":' + power
            break
    }

    jsonRequestBody += '}'

    //jsonRequestBody += ',"Power": ' + on// + '}'

    //jsonRequestBody += ',"LastCommunication":"' + tData.data.LastCommunication + '","NextCommunication":"' + tData.data.NextCommunication + '"'
    //jsonRequestBody += ',""SetFanSpeed":3,"OperationMode":1,"VaneHorizontal":3,"VaneVertical":0,"Name":null,"NumberOfFanSpeeds":5}'
    log.trace "ZXX jsonRequestBody " + jsonRequestBody
    SendJson(jsonRequestBody)
    //'{"EffectiveFlags":4,"LocalIPAddress":null,"RoomTemperature":15.5,"SetTemperature":28,"SetFanSpeed":4,"OperationMode":1,"VaneHorizontal":3,"VaneVertical":0,"Name":null,"NumberOfFanSpeeds":5,"WeatherObservations":[],"ErrorMessage":null,"ErrorCode":8000,"DefaultHeatingSetTemperature":23,"DefaultCoolingSetTemperature":21,"HideVaneControls":false,"HideDryModeControl":false,"RoomTemperatureLabel":0,"InStandbyMode":false,"TemperatureIncrementOverride":0,"DeviceID":130513,"DeviceType":0,"LastCommunication":"2019-03-21T19:01:53.96","NextCommunication":"2019-03-21T19:02:53.96","Power":false,"HasPendingCommand":false,"Offline":false,"Scene":null,"SceneOwner":null}'
}


def SendJson(String jsonBody)
{
    log.trace "sendJson() called - Request sent to Sensibo API(acStates) for PODUid : $PodUid - ${version()} - $jsonBody"
    def cmdParams = [
        uri: "${getServerUrl()}",
        path: "/Mitsubishi.Wifi.Client/Device/SetAta",
        headers: ["Content-Type": "application/json", "X-MitsContextKey":"${getapikey()}"],
        query: [apiKey:"${getapikey()}", integration:"${version()}", type:"json", fields:"acState"],
        body: jsonBody]

    //def returnStatus = -1
    try {
        httpPost(cmdParams) { resp ->
			if(resp.status == 200) {
                log.debug "updated ${resp.data}"
                debugEvent("updated ${resp.data}")
                log.trace "Successful call to Sensibo API."
				
                //returnStatus = resp.status

                log.debug "Returning True"
                return true
            }
            else {
                log.trace "Failed call to Sensibo API."
                return false
            }
        }
    }
    catch (Exception e) {
        log.debug "Exception Sending Json: " + e
        debugEvent("Exception Sending JSON: " + e)
        return false
    }

    //if (returnStatus == 200)
    //{
    //	log.debug "Returning True"
    //	return true
    //}
    //else
    //	return false
}
