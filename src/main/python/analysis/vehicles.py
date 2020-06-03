import os

import files
import pandas as pd

COL_NAMES = {
    "totalDistance": "total distance",
    "totalEmptyDistance": "total empty distance",
    "emptyRatio": "empty ratio"
}


def _get_distances_amod(alg_path):
    path = files.get_amod_csv_path(alg_path, "DistancesOverDay")
    df = files.read_amod_csv(path)
    sums = df.sum() * 1000
    sums["total empty distance"] = sums["pickup distance"] + sums["rebalancing distance"]
    sums["empty ratio"] = sums["total empty distance"] / sums["total distance"]
    return sums[list(COL_NAMES.values())]


def _get_distance_drt(alg_path):
    full_path = os.path.join(alg_path, "output", "drt_vehicle_stats_av.csv")
    df = pd.read_csv(full_path, sep=";")
    df = df.rename(columns=COL_NAMES)
    return df[list(COL_NAMES.values())].iloc[-1]


def get_distances_stats(alg_path):
    """ returns distance in meters for that algorithm"""
    if files.is_drt(alg_path):
        result = _get_distance_drt(alg_path)
    else:
        result = _get_distances_amod(alg_path)
    return result
