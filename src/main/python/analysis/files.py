import glob
import os

import pandas as pd

import constants


def is_drt(alg_path):
    return os.path.basename(alg_path).startswith(constants.DRT_NAME)


def iterate_algorithms(result_path, func):
    """ Run the guven func for all algorithms output in run_id"""
    run_id = os.path.basename(result_path)
    print(f"output for RUN_ID: {run_id}, from func: {func.__name__}")
    r = {}
    for p in glob.glob(os.path.join(result_path, "*")):
        if not os.path.isdir(p):
            continue
        r[os.path.basename(p)] = func(p)
    return r


def get_last_iteration_path(alg_path):
    """ Returns the path of the last iteration output """
    assert constants.DRT_NAME in alg_path
    path = os.path.join(alg_path, "output", "ITERS", "*")
    return max(glob.glob(path), key=lambda x: int(os.path.basename(x).split(".")[-1]))


def get_last_iter_file(alg_path, file_name):
    """ Returns the path to the last iteration version of a given file name """
    iter_path = get_last_iteration_path(alg_path)
    data_file = filter(lambda file: file.endswith(file_name), glob.glob(os.path.join(iter_path, "*")))
    return next(data_file)


def _get_column_names(dirpath):
    """ Read the description.csv file and parse the column names """
    desc = os.path.join(dirpath, "description.csv")
    if os.path.exists(desc):
        with open(desc) as desc_file:
            data = desc_file.readline()
            data = data.replace("\"", "").replace("\n", "")
            cols = data.split(", ")
    else:
        cols = None
    return cols


def get_amod_csv_path(alg_path, csv_name):
    sub_dirs = ("output", "data")
    return os.path.join(alg_path, *sub_dirs, csv_name)


def read_amod_csv(fname, default_columns=None):
    """ read amod csv file, add columns according to description.csv if exists """
    if os.path.isdir(fname):
        dirname = fname
        fname = os.path.join(fname, os.path.basename(fname)) + ".csv"
    else:
        dirname = os.path.dirname(fname)

    cols = _get_column_names(dirname)
    return pd.read_csv(fname, names=cols)
