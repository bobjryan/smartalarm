/**
 *  Smart Alarm is a versatile and highly configurable home security
 *  application for SmartThings.
 *
 *  Please visit <http://statusbits.github.io/smartalarm/> for more
 *  information.
 *
 *  Version 2.3.0 (2/18/2015)
 *
 *  The latest version of this file can be found on GitHub at:
 *  <https://github.com/statusbits/smartalarm/blob/master/SmartAlarm.groovy>
 *
 *  --------------------------------------------------------------------------
 *
 *  Copyright (c) 2014 Statusbits.com
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import groovy.json.JsonSlurper

definition(
    name: "Smart Alarm",
    namespace: "statusbits",
    author: "geko@statusbits.com",
    description: "The ultimate home security application for SmartThings.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-IsItSafe.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-IsItSafe@2x.png",
    oauth: [displayName:"Smart Alarm", displayLink:"https://github.com/statusbits/smartalarm/"]
)

preferences {
    page name:"pageSetup"
    page name:"pageAbout"
    page name:"pageSelectZones"
    page name:"pageConfigureZones"
    page name:"pageArmingOptions"
    page name:"pageAlarmOptions"
    page name:"pageNotifications"
    page name:"pageVoiceOptions"
    page name:"pageZoneStatus"
    page name:"pageRemoteControl"
    page name:"pageRestApiOptions"
}

mappings {
    path("/armaway") {
        action: [ GET: "apiArmAway" ]
    }

    path("/armaway/:pincode") {
        action: [ GET: "apiArmAway" ]
    }

    path("/armstay") {
        action: [ GET: "apiArmStay" ]
    }

    path("/armstay/:pincode") {
        action: [ GET: "apiArmStay" ]
    }

    path("/disarm") {
        action: [ GET: "apiDisarm" ]
    }

    path("/disarm/:pincode") {
        action: [ GET: "apiDisarm" ]
    }

    path("/panic") {
        action: [ GET: "apiPanic" ]
    }

    path("/status") {
        action: [ GET: "apiStatus" ]
    }
}

// Show setup page
def pageSetup() {
    LOG("pageSetup()")

    if (state.version != buildNumber()) {
        setupInit()
        return pageAbout()
    }

    def alarmStatus
    if (state.armed) {
        alarmStatus = "ARMED "
        alarmStatus += state.stay ? "STAY" : "AWAY"
    } else {
        alarmStatus = "DISARMED"
    }

    def pageProperties = [
        name:       "pageSetup",
        title:      "Status",
        nextPage:   null,
        install:    true,
        uninstall:  state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph "Smart Alarm is ${alarmStatus}"
            if (state.zones.size()) {
                href "pageZoneStatus", title:"Zone Status", description:"Tap to open"
            }
        }
        section("Setup Menu") {
            href "pageSelectZones", title:"Add/Remove Zones", description:"Tap to open"
            href "pageConfigureZones", title:"Configure Zones", description:"Tap to open"
            href "pageArmingOptions", title:"Arming/Disarming Options", description:"Tap to open"
            href "pageAlarmOptions", title:"Alarm Options", description:"Tap to open"
            href "pageNotifications", title:"Notification Options", description:"Tap to open"
            href "pageVoiceOptions", title:"Voice Notification Options", description:"Tap to open"
            href "pageRemoteControl", title:"Configure Remote Control", description:"Tap to open"
            href "pageRestApiOptions", title:"REST API Options", description:"Tap to open"
            href "pageAbout", title:"About Smart Alarm", description:"Tap to open"
        }
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

// Show "About" page
def pageAbout() {
    LOG("pageAbout()")

    def textAbout =
        "${textVersion()}\n${textCopyright()}\n\n" +
        "You can contribute to the development of this app by making " +
        "donation to geko@statusbits.com via PayPal."

    def hrefInfo = [
        url:        "http://statusbits.github.io/smartalarm/",
        style:      "embedded",
        title:      "Tap here for more information...",
        description:"http://statusbits.github.io/smartalarm/",
        required:   false,
    ]

    def pageProperties = [
        name:       "pageAbout",
        title:      "About",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textAbout
            href hrefInfo
        }
        section("License") {
            paragraph textLicense()
        }
    }
}

// Show "Zone Status" page
def pageZoneStatus() {
    LOG("pageZoneStatus()")

    def pageProperties = [
        name:       "pageZoneStatus",
        title:      "Zone Status",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        if (settings.z_contact) {
            section("Contact Sensors", hideable:true, hidden:false) {
                settings.z_contact.each() {
                    paragraph getZoneStatus(zone)
                }
            }
        }

        if (settings.z_motion) {
            section("Motion Sensors", hideable:true, hidden:false) {
                settings.z_motion.each() {
                    paragraph getZoneStatus(zone)
                }
            }
        }

        if (settings.z_movement) {
            section("Movement Sensors", hideable:true, hidden:false) {
                settings.z_movement.each() {
                    paragraph getZoneStatus(zone)
                }
            }
        }

        if (settings.z_smoke) {
            section("Smoke & CO Sensors", hideable:true, hidden:false) {
                settings.z_smoke.each() {
                    paragraph getZoneStatus(zone)
                }
            }
        }

        if (settings.z_water) {
            section("Moisture Sensors", hideable:true, hidden:false) {
                settings.z_water.each() {
                    paragraph getZoneStatus(zone)
                }
            }
        }
    }
}

// Show "Add/Remove Zones" page
def pageSelectZones() {
    LOG("pageSelectZones()")

    def helpPage =
        "A security zone is an area of your property protected by one of " +
        "the available sensors (contact, motion, movement, moisture or " +
        "smoke)."

    def inputContact = [
        name:       "z_contact",
        type:       "capability.contactSensor",
        title:      "Which contact sensors?",
        multiple:   true,
        required:   false
    ]

    def inputMotion = [
        name:       "z_motion",
        type:       "capability.motionSensor",
        title:      "Which motion sensors?",
        multiple:   true,
        required:   false
    ]

    def inputMovement = [
        name:       "z_movement",
        type:       "capability.accelerationSensor",
        title:      "Which movement sensors?",
        multiple:   true,
        required:   false
    ]

    def inputSmoke = [
        name:       "z_smoke",
        type:       "capability.smokeDetector",
        title:      "Which smoke & CO sensors?",
        multiple:   true,
        required:   false
    ]

    def inputMoisture = [
        name:       "z_water",
        type:       "capability.waterSensor",
        title:      "Which moisture sensors?",
        multiple:   true,
        required:   false
    ]

    def pageProperties = [
        name:       "pageSelectZones",
        title:      "Add/Remove Zones",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph helpPage
            input inputContact
            input inputMotion
            input inputMovement
            input inputSmoke
            input inputMoisture
        }
    }
}

// Show "Configure Zones" page
def pageConfigureZones() {
    LOG("pageConfigureZones()")

    def helpPage =
        "Each security zone can be configured as Exterior, Interior, " +
        "Entrance, Alert or Bypass.\n\n" +
        "Exterior zones are armed in both Away and Stay modes, while " +
        "Interior zones are armed only in Away mode, allowing you to move " +
        "freely inside the premises while the alarm is armed in Stay " +
        "mode.\n\n" +
        "Entrance zones allow you to enter and exit premises while the " +
        "alarm is armed without setting if off. Both entry and exit delays " +
        "are configurable.\n\n" +
        "Alert zones are always armed and are typically used for smoke and " +
        "flood alarms.\n\n" +
        "Bypass zones are never armed. This allows you to temporarily " +
        "exclude a zone from your security system."

    def zoneTypes = ["exterior", "interior", "entrance", "alert", "bypass"]

    def pageProperties = [
        name:       "pageConfigureZones",
        title:      "Configure Zones",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph helpPage
        }

        if (settings.z_contact) {
            section("Contact Sensors", hideable:true, hidden:false) {
                settings.z_contact.each() {
                    def inputContact = [
                        name:       "type_${it.id}",
                        type:       "enum",
                        title:      it.displayName,
                        metadata:   [values: zoneTypes],
                        defaultValue: "exterior",
                        required:   true
                    ]

                    input inputContact
                }
            }
        }

        if (settings.z_motion) {
            section("Motion Sensors", hideable:true, hidden:false) {
                settings.z_motion.each() {
                    def inputMotion = [
                        name:       "type_${it.id}",
                        type:       "enum",
                        title:      it.displayName,
                        metadata:   [values: zoneTypes],
                        defaultValue: "interior",
                        required:   true
                    ]

                    input inputMotion
                }
            }
        }

        if (settings.z_movement) {
            section("Movement Sensors", hideable:true, hidden:false) {
                settings.z_movement.each() {
                    def inputMovement = [
                        name:       "type_${it.id}",
                        type:       "enum",
                        title:      it.displayName,
                        metadata:   [values: zoneTypes],
                        defaultValue: "interior",
                        required:   true
                    ]

                    input inputMovement
                }
            }
        }

        if (settings.z_smoke) {
            section("Smoke & CO Sensors", hideable:true, hidden:false) {
                settings.z_smoke.each() {
                    def inputSmoke = [
                        name:       "type_${it.id}",
                        type:       "enum",
                        title:      it.displayName,
                        metadata:   [values: zoneTypes],
                        defaultValue: "alert",
                        required:   true
                    ]

                    input inputSmoke
                }
            }
        }

        if (settings.z_water) {
            section("Moisture Sensors", hideable:true, hidden:false) {
                settings.z_water.each() {
                    def inputMoisture = [
                        name:       "type_${it.id}",
                        type:       "enum",
                        title:      it.displayName,
                        metadata:   [values: zoneTypes],
                        defaultValue: "interior",
                        required:   true
                    ]

                    input inputMoisture
                }
            }
        }
    }
}

// Show "Arming/Disarming Options" page
def pageArmingOptions() {
    LOG("pageArmingOptions()")

    def helpArming =
        "Smart Alarm can be armed and disarmed by simply setting the home " +
        "'Mode'. There are two arming options - Stay and Away. Interior " +
        "zones are not armed in Stay mode, allowing you to freely move " +
        "inside your home."

    def helpExitDelay =
        "Exit delay allows you to arm the alarm and exit the premises " +
        "through one of the Entrance zones without setting off an alarm. " +
        "Exit delay is not used when arming in Stay mode."

    def helpEntryDelay =
        "Entry delay allows you to enter the premises when Smart Alarm is " +
        "armed and disarm it within specified time without setting off an " +
        "alarm. Entry delay can be optionally disabled in Stay mode."

    def inputAwayModes = [
        name:           "awayModes",
        type:           "mode",
        title:          "Arm Away in these Modes",
        multiple:       true,
        required:       false
    ]

    def inputStayModes = [
        name:           "stayModes",
        type:           "mode",
        title:          "Arm Stay in these Modes",
        multiple:       true,
        required:       false
    ]

    def inputDisarmModes = [
        name:           "disarmModes",
        type:           "mode",
        title:          "Disarm in these Modes",
        multiple:       true,
        required:       false
    ]

    def inputExitDelay = [
        name:           "exitDelay",
        type:           "enum",
        metadata:       [values:["0","15","30","45","60"]],
        title:          "Exit delay (in seconds)",
        defaultValue:   "30",
        required:       true
    ]

    def inputEntryDelay = [
        name:           "entryDelay",
        type:           "enum",
        metadata:       [values:["0","15","30","45","60"]],
        title:          "Entry delay (in seconds)",
        defaultValue:   "30",
        required:       true
    ]

    def inputEntryDelayDisable = [
        name:           "entryDelayDisable",
        type:           "bool",
        title:          "Disable in Stay mode",
        defaultValue:   false
    ]

    def pageProperties = [
        name:       "pageArmingOptions",
        title:      "Arming/Disarming Options",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph helpArming
            input inputAwayModes
            input inputStayModes
            input inputDisarmModes
        }
        section("Exit Delay") {
            paragraph helpExitDelay
            input inputExitDelay
        }
        section("Entry Delay") {
            paragraph helpEntryDelay
            input inputEntryDelay
            input inputEntryDelayDisable
        }
    }
}

// Show "Alarm Options" page
def pageAlarmOptions() {
    LOG("pageAlarmOptions()")

    def helpAlarm =
        "When an alarm is set off, Smart Alarm can turn on sirens and light" +
        "switches, take camera snapshots and execute a 'Hello, Home' action."

    def hhActions = getHelloHomeActions()
    def inputHelloHome = [
        name:           "helloHomeAction",
        type:           "enum",
        title:          "Execute this Hello Home action",
        metadata:       [values: hhActions],
        required:       false
    ]

    def inputAlarms = [
        name:           "alarms",
        type:           "capability.alarm",
        title:          "Activate these sirens",
        multiple:       true,
        required:       false
    ]

    def inputSwitches = [
        name:           "switches",
        type:           "capability.switch",
        title:          "Turn on these switches",
        multiple:       true,
        required:       false
    ]

    def inputCameras = [
        name:           "cameras",
        type:           "capability.imageCapture",
        title:          "Take camera snapshots",
        multiple:       true,
        required:       false
    ]

    def inputSirenMode = [
        name:           "sirenMode",
        type:           "enum",
        metadata:       [values:["Off","Siren","Strobe","Both"]],
        title:          "Choose siren mode",
        defaultValue:   "Both"
    ]

    def pageProperties = [
        name:       "pageAlarmOptions",
        title:      "Alarm Options",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph helpAlarm
            input inputAlarms
            input inputSirenMode
            input inputSwitches
            input inputCameras
            input inputHelloHome
        }
    }
}

// Show "Notification Options" page
def pageNotifications() {
    LOG("pageNotifications()")

    def helpAbout =
        "Smart Alarm has multiple ways of notifying you when its armed, " +
        "disarmed or when an alarm is set off, including Push " +
        "notifications, SMS (text) messages and Pushbullet notification " +
        "service."

    def inputPushAlarm = [
        name:           "pushMessage",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   true
    ]

    def inputPushStatus = [
        name:           "pushStatusMessage",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   true
    ]

    def inputPhone1 = [
        name:           "phone1",
        type:           "phone",
        title:          "Send to this number",
        required:       false
    ]

    def inputPhone1Alarm = [
        name:           "smsAlarmPhone1",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   false
    ]

    def inputPhone1Status = [
        name:           "smsStatusPhone1",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   false
    ]

    def inputPhone2 = [
        name:           "phone2",
        type:           "phone",
        title:          "Send to this number",
        required:       false
    ]

    def inputPhone2Alarm = [
        name:           "smsAlarmPhone2",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   false
    ]

    def inputPhone2Status = [
        name:           "smsStatusPhone2",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   false
    ]

    def inputPhone3 = [
        name:           "phone3",
        type:           "phone",
        title:          "Send to this number",
        required:       false
    ]

    def inputPhone3Alarm = [
        name:           "smsAlarmPhone3",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   false
    ]

    def inputPhone3Status = [
        name:           "smsStatusPhone3",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   false
    ]

    def inputPhone4 = [
        name:           "phone4",
        type:           "phone",
        title:          "Send to this number",
        required:       false
    ]

    def inputPhone4Alarm = [
        name:           "smsAlarmPhone4",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   false
    ]

    def inputPhone4Status = [
        name:           "smsStatusPhone4",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   false
    ]

    def inputPushbulletDevice = [
        name:           "pushbullet",
        type:           "device.pushbullet",
        title:          "Use these Pushbullet devices",
        multiple:       true,
        required:       false
    ]

    def inputPushbulletAlarm = [
        name:           "pushbulletAlarm",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   true
    ]

    def inputPushbulletStatus = [
        name:           "pushbulletStatus",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   true
    ]

    def pageProperties = [
        name:       "pageNotifications",
        title:      "Notification Options",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph helpAbout
        }
        section("Push Notifications") {
            input inputPushAlarm
            input inputPushStatus
        }
        section("Text Message (SMS) #1") {
            input inputPhone1
            input inputPhone1Alarm
            input inputPhone1Status
        }
        section("Text Message (SMS) #2") {
            input inputPhone2
            input inputPhone2Alarm
            input inputPhone2Status
        }
        section("Text Message (SMS) #3") {
            input inputPhone3
            input inputPhone3Alarm
            input inputPhone3Status
        }
        section("Text Message (SMS) #4") {
            input inputPhone4
            input inputPhone4Alarm
            input inputPhone4Status
        }
        section("Pushbullet Notifications") {
            input inputPushbulletDevice
            input inputPushbulletAlarm
            input inputPushbulletStatus
        }
    }
}

// Show "Voice Notification Options" page
def pageVoiceOptions() {
    LOG("pageVoiceOptions()")

    def helpAbout =
        "Smart Alarm can utilize available speech synthesis devices (e.g. " +
        "VLC Thing) to provide voice notifications."

    def inputSpeechDevice = [
        name:           "speechSynth",
        type:           "capability.speechSynthesis",
        title:          "Use these text-to-speech devices",
        multiple:       true,
        required:       false
    ]

    def inputSpeechOnAlarm = [
        name:           "speechOnAlarm",
        type:           "bool",
        title:          "Notify on Alarm",
        defaultValue:   true
    ]

    def inputSpeechOnStatus = [
        name:           "speechOnStatus",
        type:           "bool",
        title:          "Notify on Status Change",
        defaultValue:   true
    ]

    def inputSpeechTextAlarm = [
        name:           "speechText",
        type:           "text",
        title:          "Alarm Phrase",
        required:       false
    ]

    def inputSpeechTextArmedAway = [
        name:           "speechTextArmedAway",
        type:           "text",
        title:          "Armed Away Phrase",
        required:       false
    ]

    def inputSpeechTextArmedStay = [
        name:           "speechTextArmedStay",
        type:           "text",
        title:          "Armed Stay Phrase",
        required:       false
    ]

    def inputSpeechTextDisarmed = [
        name:           "speechTextDisarmed",
        type:           "text",
        title:          "Disarmed Phrase",
        required:       false
    ]

    def pageProperties = [
        name:       "pageNotifications",
        title:      "Notification Options",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph helpAbout
            input inputSpeechDevice
            input inputSpeechOnAlarm
            input inputSpeechOnStatus
            input inputSpeechTextAlarm
            input inputSpeechTextArmedAway
            input inputSpeechTextArmedStay
            input inputSpeechTextDisarmed
        }
    }
}

// Show "Configure Remote Control" page
def pageRemoteControl() {
    LOG("pageRemoteControl()")

    def textHelp =
        "You can use remote controls such as Aeon Labs Minimote to arm " +
        "and disarm Smart Alarm."

    def inputButtons = [
        name:       "buttons",
        type:       "capability.button",
        title:      "Which remote controls?",
        multiple:   true,
        required:   false
    ]

    def inputArmAway = [
        name:           "buttonArmAway",
        type:           "number",
        title:          "Which button?",
        required:       false
    ]

    def inputHoldArmAway = [
        name:           "holdArmAway",
        type:           "bool",
        title:          "Use button 'Hold' action",
        defaultValue:   false,
        required:       true
    ]

    def inputArmStay = [
        name:           "buttonArmStay",
        type:           "number",
        title:          "Which button?",
        required:       false
    ]

    def inputHoldArmStay = [
        name:           "holdArmStay",
        type:           "bool",
        title:          "Use button 'Hold' action",
        defaultValue:   false,
        required:       true
    ]

    def inputDisarm = [
        name:           "buttonDisarm",
        type:           "number",
        title:          "Which button?",
        required:       false
    ]

    def inputHoldDisarm = [
        name:           "holdDisarm",
        type:           "bool",
        title:          "Use button 'Hold' action",
        defaultValue:   false,
        required:       true
    ]

    def inputPanic = [
        name:           "buttonPanic",
        type:           "number",
        title:          "Which button?",
        required:       false
    ]

    def inputHoldPanic = [
        name:           "holdPanic",
        type:           "bool",
        title:          "Use button 'Hold' action",
        defaultValue:   false,
        required:       true
    ]

    def pageProperties = [
        name:       "pageRemoteControl",
        title:      "Configure Remote Control",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textHelp
            input inputButtons
        }

        section("Arm Away Button") {
            input inputArmAway
            input inputHoldArmAway
        }

        section("Arm Stay Button") {
            input inputArmStay
            input inputHoldArmStay
        }

        section("Disarm Button") {
            input inputDisarm
            input inputHoldDisarm
        }

        section("Panic Button") {
            input inputPanic
            input inputHoldPanic
        }
    }
}

// Show "REST API Options" page
def pageRestApiOptions() {
    LOG("pageRestApiOptions()")

    def textHelp =
        "Smart Alarm can be controlled remotely by any Web client using " +
        "REST API. Please refer to Smart Alarm documentation for more " +
        "information.\n\n" +
        "WARNING: Make sure OAuth is enabled in the smart app settings " +
        "(in SmartThings IDE) before enabling REST API."

    def textPincode =
        "You can specify optional PIN code to protect arming and disarming " +
        "Smart Alarm via REST API from unauthorized access. If set, the " +
        "PIN code is always required for disarming Smart Alarm, however " +
        "you can optionally turn it off for arming Smart Alarm."

    def inputRestApi = [
        name:           "restApiEnabled",
        type:           "bool",
        title:          "Enable REST API",
        defaultValue:   false
    ]

    def inputPincode = [
        name:           "pincode",
        type:           "number",
        title:          "PIN Code",
        required:       false
    ]

    def inputArmWithPin = [
        name:           "armWithPin",
        type:           "bool",
        title:          "Require PIN code to arm",
        defaultValue:   true
    ]

    def pageProperties = [
        name:       "pageRestApiOptions",
        title:      "REST API Options",
        nextPage:   "pageSetup",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textHelp
            input inputRestApi
            paragraph textPincode
            input inputPincode
            input inputArmWithPin
        }

        if (isRestApiEnabled()) {
            section("REST API Info") {
                paragraph "App ID:\n${app.id}"
                paragraph "Access Token:\n${state.accessToken}"
            }
        }
    }
}

def installed() {
    LOG("installed()")

    initialize()
    state.installed = true
}

def updated() {
    LOG("updated()")

    unsubscribe()
    //unschedule()
    initialize()
}

private def setupInit() {
    LOG("setupInit()")

    state.version = buildNumber()
    if (state.installed == null) {
        state.installed = false
        state.armed = false
        state.alarm = null
        state.zones = []
    }
}

private def initialize() {
    log.info "${app.name}. ${textVersion()}. ${textCopyright()}"
    log.debug "initialize with ${settings}"

    state._init_ = true
    state.exitDelay = settings.exitDelay?.toInteger() ?: 0
    state.entryDelay = settings.entryDelay?.toInteger() ?: 0
    state.armDelay = false
    state.offSwitches = []
    state.history = []

    if (settings.awayModes?.contains(location.mode)) {
        state.armed = true
        state.stay = false
    } else if (settings.stayModes?.contains(location.mode)) {
        state.armed = true
        state.stay = true
    } else {
        state.armed = false
        state.stay = false
    }

    initControlPanel()
    initZones()
    initButtons()
    initRestApi()
    resetPanel()
    subscribe(location, onLocation)

    STATE()
    state._init_ = false
}

private def initControlPanel() {
    LOG("initControlPanel()")

    if (state.controlPanel) {
        def cp = getChildDevice(state.controlPanel)
        if (!cp) {
            log.warn "Control panel not found"
            state.controlPanel = null
        }
    }

    if (state.controlPanel == null) {
        state.controlPanel = createControlPanel(app.name)
    }
}

private def createControlPanel(name) {
    LOG("createControlPanel(${name})")

    def dni = createNetworkId()
    def devFile = "SmartAlarm Control Panel"
    def devParams = [
        name:           name,
        label:          name,
        completedSetup: true
    ]

    try {
        def dev = addChildDevice("statusbits", devFile, dni, null, devParams)
        log.info "Created control panel. DNI: ${dev.deviceNetworkId}"
    } catch (e) {
        dni = null
        log.error "Cannot create control panel."
        log.error e
    }

    return dni
}

private def updateControlPanel() {
    LOG("updateControlPanel()")

    if (state.controlPanel == null) {
        return
    }

    def cp = getChildDevice(state.controlPanel)
    if (!cp) {
        log.warn "Control panel not found"
        state.controlPanel = null
        return
    }

    if (state.alarm) {
        cp.parse("status: alarm, zone: ${state.alarm}")
        return
    }

    if (state.armed) {
        def mode = state.stay ? "stay" : "away"
        cp.parse("status: armed, mode: ${mode}")
    } else {
        cp.parse("status: disarmed")
    }
}

private def initZones() {
    LOG("initZones()")

    state.zones = []

    if (settings.z_contact) {
        settings.z_contact.each() {
            state.zones << [
                deviceId:   it.id,
                sensorType: "contact",
                zoneType:   settings["type_${it.id}"] ?: "exterior"
            ]
        }
        subscribe(settings.z_contact, "contact.open", onContact)
    }

    if (settings.z_motion) {
        settings.z_motion.each() {
            state.zones << [
                deviceId:   it.id,
                sensorType: "motion",
                zoneType:   settings["type_${it.id}"] ?: "interior"
            ]
        }
        subscribe(settings.z_motion, "motion.active", onMotion)
    }

    if (settings.z_movement) {
        settings.z_movement.each() {
            state.zones << [
                deviceId:   it.id,
                sensorType: "movement",
                zoneType:   settings["type_${it.id}"] ?: "interior"
            ]
        }
        subscribe(settings.z_movement, "acceleration.active", onMovement)
    }

    if (settings.z_smoke) {
        settings.z_smoke.each() {
            state.zones << [
                deviceId:   it.id,
                sensorType: "smoke",
                zoneType:   settings["type_${it.id}"] ?: "alert"
            ]
        }
        subscribe(settings.z_smoke, "smoke.detected", onSmoke)
        subscribe(settings.z_smoke, "smoke.tested", onSmoke)
        subscribe(settings.z_smoke, "carbonMonoxide.detected", onSmoke)
        subscribe(settings.z_smoke, "carbonMonoxide.tested", onSmoke)
    }

    if (settings.z_water) {
        settings.z_water.each() {
            state.zones << [
                deviceId:   it.id,
                sensorType: "water",
                zoneType:   settings["type_${it.id}"] ?: "alert"
            ]
        }
        subscribe(settings.z_water, "water.wet", onWater)
    }
}

private def initButtons() {
    LOG("initButtons()")

    state.buttonActions = []
    if (settings.buttons) {
        if (settings.buttonArmAway) {
            def button = settings.buttonArmAway.toInteger()
            def event = settings.holdArmAway ? "held" : "pushed"
            state.buttonActions.add([button:button, event:event, action:"armAway"])
        }

        if (settings.buttonArmStay) {
            def button = settings.buttonArmStay.toInteger()
            def event = settings.holdArmStay ? "held" : "pushed"
            state.buttonActions.add([button:button, event:event, action:"armStay"])
        }

        if (settings.buttonDisarm) {
            def button = settings.buttonDisarm.toInteger()
            def event = settings.holdDisarm ? "held" : "pushed"
            state.buttonActions.add([button:button, event:event, action:"disarm"])
        }

        if (settings.buttonPanic) {
            def button = settings.buttonPanic.toInteger()
            def event = settings.holdPanic ? "held" : "pushed"
            state.buttonActions.add([button:button, event:event, action:"panic"])
        }

        if (state.buttonActions) {
            subscribe(settings.buttons, "button", onButtonEvent)
        }
    }
}

private def initRestApi() {
    if (settings.restApiEnabled) {
        if (!state.accessToken) {
            def token = createAccessToken()
            LOG("Created new access token: ${token})")
        }
        state.url = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/"
        log.info "REST API enabled"
    } else {
        state.url = ""
        log.info "REST API disabled"
    }
}

private def isRestApiEnabled() {
    return settings.restApiEnabled && state.accessToken
}

def resetPanel() {
    LOG("resetPanel()")

    state.alarm = null
    settings.alarms*.off()

    unschedule()

    // Turn off only those switches that we've turned on
    def switchesOff = state.offSwitches
    if (switchesOff) {
        LOG("switchesOff: ${switchesOff}")
        settings.switches.each() {
            if (switchesOff.contains(it.id)) {
                it.off()
            }
        }
        state.offSwitches = []
    }

    // Schedule delayed arming of Entrance zones
    if (state.armed && !state.stay && state.exitDelay) {
        state.armDelay = true
        myRunIn(state.exitDelay, armEntranceZones)
    } else {
        state.armDelay = false
    }

    updateControlPanel()

    // Send notification
    def msg = "${location.name} alarm is "
    if (state.armed) {
        def mode = state.stay ? "Stay" : "Away"
        msg += "Armed ${mode}."
    } else {
        msg += "Disarmed."
    }

    log.trace msg
    notify(msg)
    notifyVoice()
}

private def onZoneEvent(evt, sensorType) {
    LOG("onZoneEvent(${evt.displayName}, ${sensorType})")

    def zone = getZoneForDevice(evt.deviceId, sensorType)
    if (!zone) {
        log.warn "Cannot find zone for device ${evt.deviceId}"
        return
    }

    if (isZoneArmed(zone)) {
        if (!state.alarm) {
            // Activate alarm
            state.alarm = evt.displayName
            if (zoneType == "entrance" && state.entryDelay &&
                !(state.stay && settings.entryDelayDisable)) {
                myRunIn(state.entryDelay, activateAlarm)
            } else {
                activateAlarm()
            }
        }
    }
}

def onContact(evt)  { onZoneEvent(evt, "contact") }
def onMotion(evt)   { onZoneEvent(evt, "motion") }
def onMovement(evt) { onZoneEvent(evt, "movement") }
def onSmoke(evt)    { onZoneEvent(evt, "smoke") }
def onWater(evt)    { onZoneEvent(evt, "water") }

def onLocation(evt) {
    LOG("onLocation(${evt.value})")

    String mode = evt.value
    if (settings.awayModes?.contains(mode)) {
        armAway()
    } else if (settings.stayModes?.contains(mode)) {
        armStay()
    } else if (settings.disarmModes?.contains(mode)) {
        disarm()
    }
}

def onButtonEvent(evt) {
    LOG("onButtonEvent(${evt.displayName})")

    if (!state.buttonActions || !evt.data) {
        return
    }

    def slurper = new JsonSlurper()
    def data = slurper.parseText(evt.data)
    def button = data.buttonNumber?.toInteger()
    if (button) {
        LOG("Button '${button}' was ${evt.value}.")
        def item = state.buttonActions.find {
            it.button == button && it.event == evt.value
        }

        if (item) {
            LOG("Executing '${item.action}' button action")
            "${item.action}"()
        }
    }
}

def armAway() {
    LOG("armAway()")

    if (!state.armed || state.stay) {
        state.armed = true
        state.stay = false
        resetPanel()
    }
}

def armStay() {
    LOG("armStay()")

    if (!state.armed || !state.stay) {
        state.armed = true
        state.stay = true
        resetPanel()
    }
}

def disarm() {
    LOG("disarm()")

    if (state.armed) {
        state.armed = false
        resetPanel()
    }
}

def panic() {
    LOG("panic()")

    state.alarm = "Panic";
    activateAlarm()
}

def armEntranceZones() {
    LOG("armEntranceZones()")

    if (state.armed) {
        state.armDelay = false

        def msg = "Entrance zones armed"
        log.info msg
        notify(msg)
    }
}

// .../armaway REST API endpoint
def apiArmAway() {
    LOG("apiArmAway()")

    if (!isRestApiEnabled()) {
        log.error "REST API disabled"
        return httpError(403, "Access denied")
    }

    if (settings.pincode && settings.armWithPin) {
        if (params.pincode != settings.pincode.toString()) {
            log.error "Invalid PIN code '${params.pincode}'"
            return httpError(403, "Access denied")
        }
    }

    armAway()
    return apiStatus()
}

// .../armstay REST API endpoint
def apiArmStay() {
    LOG("apiArmStay()")

    if (!isRestApiEnabled()) {
        log.error "REST API disabled"
        return httpError(403, "Access denied")
    }

    if (settings.pincode && settings.armWithPin) {
        if (params.pincode != settings.pincode.toString()) {
            log.error "Invalid PIN code '${params.pincode}'"
            return httpError(403, "Access denied")
        }
    }

    armStay()
    return apiStatus()
}

// .../disarm REST API endpoint
def apiDisarm() {
    LOG("apiDisarm()")

    if (!isRestApiEnabled()) {
        log.error "REST API disabled"
        return httpError(403, "Access denied")
    }

    if (settings.pincode) {
        if (params.pincode != settings.pincode.toString()) {
            log.error "Invalid PIN code '${params.pincode}'"
            return httpError(403, "Access denied")
        }
    }

    disarm()
    return apiStatus()
}

// .../panic REST API endpoint
def apiPanic() {
    LOG("apiPanic()")

    if (!isRestApiEnabled()) {
        log.error "REST API disabled"
        return httpError(403, "Access denied")
    }

    panic()
    return apiStatus()
}

// .../status REST API endpoint
def apiStatus() {
    LOG("apiStatus()")

    if (!isRestApiEnabled()) {
        log.error "REST API disabled"
        return httpError(403, "Access denied")
    }

    def status = [
        status: state.armed ? (state.stay ? "armed stay" : "armed away") : "disarmed",
        alarm:  state.alarm
    ]

    return status
}

def activateAlarm() {
    LOG("activateAlarm()")

    if (!state.alarm) {
        log.warn "activateAlarm: false alarm"
        return
    }

    switch (settings.sirenMode) {
    case "Siren":
        settings.alarms*.siren()
        break

    case "Strobe":
        settings.alarms*.strobe()
        break
        
    case "Both":
        settings.alarms*.both()
        break
    }

    // Only turn on those switches that are currently off
    def switchesOn = settings.switches?.findAll { it?.currentSwitch == "off" }
    LOG("switchesOn: ${switchesOn}")
    if (switchesOn) {
        switchesOn*.on()
        state.offSwitches = switchesOn.collect { it.id }
    }

    settings.cameras*.take()

    if (settings.helloHomeAction) {
        log.info "Executing HelloHome action '${settings.helloHomeAction}'"
        location.helloHome.execute(settings.helloHomeAction)
    }

    def msg = "Alarm at ${location.name}!\n${state.alarm}"
    log.info msg
    notify(msg)
    notifyVoice()

    myRunIn(180, resetPanel)
}

private def notify(msg) {
    LOG("notify(${msg})")
    if (state.alarm) {
        // Alarm notification
        if (settings.pushMessage) {
            mySendPush(msg)
        } else {
            sendNotificationEvent(msg)
        }

        if (settings.smsAlarmPhone1 && settings.phone1) {
            sendSms(phone1, msg)
        }

        if (settings.smsAlarmPhone2 && settings.phone2) {
            sendSms(phone2, msg)
        }

        if (settings.smsAlarmPhone3 && settings.phone3) {
            sendSms(phone3, msg)
        }

        if (settings.smsAlarmPhone4 && settings.phone4) {
            sendSms(phone4, msg)
        }

        if (settings.pushbulletAlarm && settings.pushbullet) {
            settings.pushbullet*.push(msg)
        }    
    } else {
        // Status change notification
        if (settings.pushStatusMessage) {
            mySendPush(msg)
        } else {
            sendNotificationEvent(msg)
        }

        if (settings.smsStatusPhone1 && settings.phone1) {
            sendSms(phone1, msg)
        }

        if (settings.smsStatusPhone2 && settings.phone2) {
            sendSms(phone2, msg)
        }

        if (settings.smsStatusPhone3 && settings.phone3) {
            sendSms(phone3, msg)
        }

        if (settings.smsStatusPhone4 && settings.phone4) {
            sendSms(phone4, msg)
        }

        if (settings.pushbulletStatus && settings.pushbullet) {
            settings.pushbullet*.push(msg)
        }
    }
}

private def notifyVoice() {
    LOG("notifyVoice()")

    if (!settings.speechSynth || state._init_) {
        return
    }

    def phrase = null
    if (state.alarm) {
        // Alarm notification
        if (settings.speechOnAlarm) {
            phrase = settings.speechText ?: getStatusPhrase()
        }
    } else {
        // Status change notification
        if (settings.speechOnStatus) {
            if (state.armed) {
                if (state.stay) {
                    phrase = settings.speechTextArmedStay ?: getStatusPhrase()
                } else {
                    phrase = settings.speechTextArmedAway ?: getStatusPhrase()
                }
            } else {
                phrase = settings.speechTextDisarmed ?: getStatusPhrase()
            }
        }
    }

    if (phrase) {
        settings.speechSynth*.speak(phrase)
    }
}

private def getStatusPhrase() {
    LOG("getStatusPhrase()")

    def phrase = ""
    if (state.alarm) {
        phrase = "Alarm in zone ${state.alarm} at ${location.name}!"
    } else {
        phrase = "${location.name} security is "
        if (state.armed) {
            def mode = state.stay ? "stay" : "away"
            phrase += "armed in ${mode} mode."
        } else {
            phrase += "disarmed."
        }
    }

    return phrase
}

private def getHelloHomeActions() {
    def actions = location.helloHome?.getPhrases().collect() { it.label }
    return actions.sort()
}

private def isZoneArmed(zone) {
    switch (zone.zoneType) {
    case "alert":
        return true

    case "exterior":
        return state.armed

    case "interior":
        return (state.armed && !state.stay)

    case "entrance":
        return (state.armed && !state.armDelay)
    }

    return false
}

private def isZoneOpen(zone) {
    return false
}

private def getZoneStatus(id, sensorType) {
    def zone = getZoneForDevice(id, sensorType)
    if (!zone) {
        return "Zone '${it.displayName}' not found"
    }

    def armed = isZoneArmed(zone)
    def open = isZoneOpen(zone)
    def str = "${it.displayName}\n${zone.zoneType}, "
    str += armed ? "Armed, " : "Disarmed, "
    str += open ? "Open" : "Closed"

    return str
}

private def getZoneForDevice(id, sensorType) {
    return state.zones.find() { it.deviceId == id && it.sensorType == sensorType }
}

private def getDeviceById(id) {
    def device = settings.z_contact?.find() { it.id == id }
    if (device) {
        return device
    }

    device = settings.z_motion?.find() { it.id == id }
    if (device) {
        return device
    }

    device = settings.z_movement?.find() { it.id == id }
    if (device) {
        return device
    }

    device = settings.z_smoke?.find() { it.id == id }
    if (device) {
        return device
    }

    device = settings.z_water?.find() { it.id == id }

    return device
}

private def myRunIn(delay_s, func) {
    if (delay_s > 0) {
        def date = new Date(now() + (delay_s * 1000))
        runOnce(date, func)
        LOG("scheduled '${func}' to run at ${date}")
    }
}

private def mySendPush(msg) {
    // cannot call sendPush() from installed() or updated()
    if (!state._init_) {
        // sendPush can throw an exception
        try {
            sendPush(msg)
        } catch (e) {
            log.error e
        }
    }
}

private def history(String event, String description = "") {
    def history = state.history
    history << [time: now(), event: event, description: description]
    if (history.size() > 10) {
        history = history.sort{it.time}
        history = history[1..-1]
    }

    LOG("history: ${history}")
    state.history = history
}

private def createNetworkId() {
    String hexchars = "0123456789ABCDEF"
    Random rand = new Random(now())
    def chars = (0..7).collect { hexchars[rand.nextInt(16)] }
    return chars.join()
}

private def buildNumber() {
    return 150218
}

private def textVersion() {
    def text = "Version 2.3.0 (2/18/2015)"
}

private def textCopyright() {
    def text = "Copyright © 2014 Statusbits.com"
}

private def textLicense() {
    def text =
        "This program is free software: you can redistribute it and/or " +
        "modify it under the terms of the GNU General Public License as " +
        "published by the Free Software Foundation, either version 3 of " +
        "the License, or (at your option) any later version.\n\n" +
        "This program is distributed in the hope that it will be useful, " +
        "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU " +
        "General Public License for more details.\n\n" +
        "You should have received a copy of the GNU General Public License " +
        "along with this program. If not, see <http://www.gnu.org/licenses/>."
}

private def LOG(message) {
    log.trace message
}

private def STATE() {
    log.trace "state: ${state}"
}
