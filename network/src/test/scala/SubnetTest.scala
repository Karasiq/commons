import java.net.InetAddress

import org.scalatest.FlatSpec

import com.karasiq.networkutils.ip.Subnet

class SubnetTest extends FlatSpec {
  "Subnet parser" should "read IPv4 range" in {
    val subnet = Subnet("10.0.0.0/8")
    assert(subnet.isInRange(InetAddress.getByName("10.1.2.3")))
    assert(!subnet.isInRange(InetAddress.getByName("11.1.2.3")))
  }

  it should "read IPv6 range" in {
    val subnet = Subnet("::ffff:0:0/96")
    assert(subnet.isInRange(InetAddress.getByName("::ffff:0.0.0.0")))
    assert(!subnet.isInRange(InetAddress.getByName("64:ff9b::0.0.0.0")))
  }
}
