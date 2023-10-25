function start(ms) {
    importClass(android.graphics.Point);
    var pos = new Point(171, 50);
    var mobId = 9400611;
    var mobName = "Crocell";

    var player = ms.getPlayer();
    var map = player.getMap();

    if (map.getMonsterById(mobId) != null) {
        return;
    }

    importPackage(Packages.server.life);
    map.spawnMonsterOnGroundBelow(LifeFactory.getMonster(mobId), pos);
    player.message(mobName + " has appeared!");
}