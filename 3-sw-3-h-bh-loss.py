from mininet.topo import Topo

class MyTopo( Topo ):
    "Simple topology example."

    def __init__( self ):
        "Create 3-sw-3-h-blackhole topo."

        # Initialize topology
        Topo.__init__( self )

        # Add hosts and switches
        topHost = self.addHost( 'h1' )
        rightHost = self.addHost( 'h2' )
        bottomHost = self.addHost( 'h3' )
        topSwitch = self.addSwitch( 's1' )
        rightSwitch = self.addSwitch( 's2' )
        bottomSwitch = self.addSwitch( 's3' )

        # Add links
        self.addLink( topHost, topSwitch, bw=1, delay='10ms')
        self.addLink( rightHost, rightSwitch, bw=1, delay='10ms')
        self.addLink( bottomHost,  bottomSwitch, bw=1, delay='10ms')
        self.addLink( topSwitch, rightSwitch, bw=1, delay='10ms', loss=20 )
        self.addLink( rightSwitch, bottomSwitch, bw=1, delay='10ms', loss=20 )


topos = { 'mytopo': ( lambda: MyTopo() ) }
