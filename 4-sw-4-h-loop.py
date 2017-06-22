from mininet.topo import Topo

class MyTopo( Topo ):
    "Simple topology example."

    def __init__( self ):
        "Create 4-sw-4-h-loop topo."

        # Initialize topology
        Topo.__init__( self )

        # Add hosts and switches
        topHost = self.addHost( 'h1' )
        rightHost = self.addHost( 'h2' )
        bottomHost = self.addHost( 'h3' )
        leftHost = self.addHost( 'h4' )
        topSwitch = self.addSwitch( 's1' )
        rightSwitch = self.addSwitch( 's2' )
        bottomSwitch = self.addSwitch( 's3' )
        leftSwitch = self.addSwitch( 's4' )

        # Add links
        self.addLink( topHost, topSwitch )
        self.addLink( rightHost, rightSwitch )
        self.addLink( bottomHost,  bottomSwitch )
        self.addLink( leftHost, leftSwitch )
        self.addLink( topSwitch, rightSwitch )
        self.addLink( rightSwitch, bottomSwitch )
        self.addLink( bottomSwitch, leftSwitch )
        self.addLink( leftSwitch, topSwitch )


topos = { 'mytopo': ( lambda: MyTopo() ) }
