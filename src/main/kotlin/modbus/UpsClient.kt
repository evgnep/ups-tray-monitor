package modbus

import com.ghgande.j2mod.modbus.Modbus
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster
import com.ghgande.j2mod.modbus.util.SerialParameters

// Status read from the UPS. See PROJECT_BRIEF.md register map.
data class UpsStatus(
    val onBattery: Boolean,
    val capacityPercent: Double
)

// Talks to the UPS over Modbus ASCII. Confirmed working params:
// 9600 baud, 8N1, slave id 1 (see PROJECT_BRIEF.md and ModbusPollTest.kt).
class UpsClient {
    private var master: ModbusSerialMaster? = null

    val isConnected: Boolean
        get() = master?.isConnected == true

    fun connect(portName: String, baudRate: Int) {
        disconnect()
        val params = SerialParameters()
        params.portName = portName
        params.setBaudRate(baudRate)
        params.setDatabits(8)
        params.setParity("None")
        params.setStopbits("1")
        params.setEncoding(Modbus.SERIAL_ENCODING_ASCII)
        params.setEcho(false)

        val newMaster = ModbusSerialMaster(params, 2000)
        newMaster.connect()
        master = newMaster
    }

    fun disconnect() {
        val current = master
        master = null
        if (current != null && current.isConnected) {
            current.disconnect()
        }
    }

    fun readStatus(slaveId: Int = 1): UpsStatus {
        val current = master ?: error("UPS client is not connected")
        // one request covers addr 1 (mains voltage) .. 56 (battery capacity)
        val registers = current.readMultipleRegisters(slaveId, 1, 56)
        val mainsRaw = registers[0].value // addr 1
        val capacityRaw = registers[55].value // addr 56
        return UpsStatus(
            onBattery = mainsRaw == 0,
            capacityPercent = capacityRaw / 10.0
        )
    }
}
