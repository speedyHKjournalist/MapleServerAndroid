function start(ms) {
    importClass(android.graphics.Point);
    var pos = new Point(461, 61);
    var mobId = 9400612;
    var mobName = "Marbas";

    var player = ms.getPlayer();
    var map = player.getMap();

    if (map.getMonsterById(mobId) != null) {
        return;
    }

    importPackage(Packages.server.life);
    map.spawnMonsterOnGroundBelow(LifeFactory.getMonster(mobId), pos);
    player.message(mobName + " has appeared!");
}