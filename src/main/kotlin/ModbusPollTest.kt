import com.ghgande.j2mod.modbus.Modbus
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster
import com.ghgande.j2mod.modbus.util.SerialParameters

// Quick connectivity test for the UPS on COM4.
// Reads holding registers 1..80 with FC3, same as the native utility does
// (see PROJECT_BRIEF.md "Опрос в родной утилите"). Prints raw values so we
// can check if we are talking to the device at all before trusting any
// scaling/formula.
//
// Port settings are NOT confirmed yet (brief says native tool shows "Auto").
// We try 9600 baud first (7E1 and 8N1 - the two classic Modbus ASCII
// defaults), same as suggested in PROJECT_BRIEF.md. If none respond, try
// other baud rates: 2400/4800/19200.
val PORT_NAME = "COM4"
val SLAVE_ID = 1
val START_ADDRESS = 1
val QUANTITY = 80

data class PortConfig(val baud: Int, val databits: Int, val parity: String, val stopbits: String) {
    override fun toString() = "$baud ${databits}${parity[0]}${stopbits}"
}

fun buildParams(cfg: PortConfig): SerialParameters {
    val params = SerialParameters()
    params.portName = PORT_NAME
    params.setBaudRate(cfg.baud)
    params.setDatabits(cfg.databits)
    params.setParity(cfg.parity)
    params.setStopbits(cfg.stopbits)
    params.setEncoding(Modbus.SERIAL_ENCODING_ASCII)
    params.setEcho(false)
    return params
}

// Tries one combo with a single-register read (fast fail). Returns the
// master still connected on success, or null on failure.
fun tryConfig(cfg: PortConfig): ModbusSerialMaster? {
    print("Trying $cfg ... ")
    val master = ModbusSerialMaster(buildParams(cfg), 1500)
    try {
        master.connect()
        val regs = master.readMultipleRegisters(SLAVE_ID, START_ADDRESS, 1)
        println("OK, addr=$START_ADDRESS value=${regs[0].value}")
        return master
    } catch (e: Exception) {
        println("no response (${e.javaClass.simpleName})")
        if (master.isConnected) master.disconnect()
        return null
    }
}

fun dumpFullRange(master: ModbusSerialMaster) {
    println("Reading holding registers $START_ADDRESS..${START_ADDRESS + QUANTITY - 1} (FC3)")
    val registers = master.readMultipleRegisters(SLAVE_ID, START_ADDRESS, QUANTITY)

    println("--- raw values ---")
    for ((i, reg) in registers.withIndex()) {
        val addr = START_ADDRESS + i
        println("addr=$addr value=${reg.value}")
    }

    // known registers from PROJECT_BRIEF.md, for a quick sanity check
    val mainsRaw = registers[1 - START_ADDRESS].value
    val battVoltage = registers[50 - START_ADDRESS].value / 10.0
    val capacity = registers[56 - START_ADDRESS].value / 10.0
    println("--- decoded (see PROJECT_BRIEF.md) ---")
    println("Mains voltage: ${mainsRaw / 10.0} V (raw=$mainsRaw)")
    println("Battery voltage: $battVoltage V")
    println("Battery capacity: $capacity %")
    println("On battery (mains raw == 0): ${mainsRaw == 0}")
}

fun main() {
    // start with 9600 baud, both classic framings, as requested
    val combos = listOf(
        PortConfig(9600, 7, "Even", "1"),
        PortConfig(9600, 8, "None", "1"),
    )

    println("Scanning serial params on $PORT_NAME, slave id $SLAVE_ID ...")
    var working: ModbusSerialMaster? = null
    for (cfg in combos) {
        working = tryConfig(cfg)
        if (working != null) break
    }

    if (working == null) {
        println("No response at 9600 baud (7E1/8N1). Try other baud rates: 2400/4800/19200.")
        return
    }

    try {
        dumpFullRange(working)
    } catch (e: Exception) {
        println("FAILED on full read: ${e.javaClass.simpleName}: ${e.message}")
    } finally {
        working.disconnect()
    }
}
