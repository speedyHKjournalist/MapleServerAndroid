function start(ms) {
    var mobId = 3300000 + (Math.floor(Math.random() * 3) + 5);
    var player = ms.getPlayer();
    var map = player.getMap();

    importPackage(Packages.server.life);
    importClass(android.graphics.Point);
    map.spawnMonsterOnGroundBelow(LifeFactory.getMonster(mobId), new Point(-28, -67));
}